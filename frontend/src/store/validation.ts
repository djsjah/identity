import { makeAutoObservable, runInAction } from "mobx";
import { ApiError, apiRequest } from "../api/client";
import { authStore } from "./auth";

export type ValidationResult = {
  result: "Fake" | "Real";
  probability: number;
};

type ImageStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
type ImageUploadResponse = { imageId: string; status: ImageStatus };
type ImageStatusResponse = {
  imageId: string;
  status: ImageStatus;
  predictedClass: number | null;
  classDescription: string | null;
  fakeProbability: number | null;
  error: string | null;
};

export enum ErrorType {
  NoFaceDetected = "NoFaceDetected",
  InvalidFile = "InvalidFile",
  FileTooLarge = "FileTooLarge",
  NetworkError = "NetworkError",
  Unauthorized = "Unauthorized",
  CommonError = "CommonError",
}

type ValidationPhase =
  "idle" | "uploading" | "pending" | "processing" | "completed" | "failed";

const IMAGE_ID_KEY = "identity-image-id";
const MAX_FILE_SIZE = 5 * 1024 * 1024;
const POLL_INTERVAL_MS = 2000;
const MAX_POLL_FAILURES = 5;

let requestController: AbortController | null = null;
let pollTimer: ReturnType<typeof setTimeout> | null = null;
let requestRevision = 0;
let pollFailures = 0;

class ValidationStore {
  file: File | null = null;
  imageId: string | null = sessionStorage.getItem(IMAGE_ID_KEY);
  result: ValidationResult | null = null;
  errorStatus: ErrorType | null = null;
  phase: ValidationPhase = this.imageId ? "pending" : "idle";
  constructor() {
    makeAutoObservable(this, {}, { autoBind: true });
  }

  get loading() {
    return (
      this.phase === "uploading" ||
      this.phase === "pending" ||
      this.phase === "processing"
    );
  }

  reset() {
    requestRevision++;
    requestController?.abort();
    if (pollTimer) clearTimeout(pollTimer);
    requestController = null;
    pollTimer = null;
    pollFailures = 0;
    sessionStorage.removeItem(IMAGE_ID_KEY);
    this.imageId = null;
    this.file = null;
    this.result = null;
    this.errorStatus = null;
    this.phase = "idle";
  }

  async validate(file: File) {
    this.reset();
    this.file = file;

    if (!file.size || !["image/jpeg", "image/png"].includes(file.type)) {
      this.phase = "failed";
      this.errorStatus = ErrorType.InvalidFile;
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      this.phase = "failed";
      this.errorStatus = ErrorType.FileTooLarge;
      return;
    }

    this.phase = "uploading";
    const revision = requestRevision;
    const controller = new AbortController();
    requestController = controller;

    try {
      const body = new FormData();
      body.append("file", file, file.name);
      const data = await apiRequest<ImageUploadResponse>("/images/upload", {
        method: "POST",
        body,
        signal: controller.signal,
      });

      if (revision !== requestRevision) return;
      if (
        !data ||
        typeof data.imageId !== "string" ||
        !data.imageId ||
        data.status !== "PENDING"
      ) {
        throw new Error("Invalid upload response");
      }

      runInAction(() => {
        this.imageId = data.imageId;
        this.phase = "pending";
        sessionStorage.setItem(IMAGE_ID_KEY, data.imageId);
      });

      void this.poll(revision);
    } catch (error) {
      if (revision !== requestRevision || controller.signal.aborted) return;
      runInAction(() => this.setRequestError(error));
    }
  }

  resume() {
    if (this.imageId && this.loading && !requestController && !pollTimer) {
      void this.poll(requestRevision);
    }
  }

  retry() {
    if (this.imageId) {
      this.errorStatus = null;
      this.phase = "pending";
      pollFailures = 0;
      void this.poll(requestRevision);
    } else if (this.file) {
      void this.validate(this.file);
    }
  }

  private async poll(revision: number) {
    if (
      revision !== requestRevision ||
      !this.imageId ||
      !authStore.isAuthenticated
    )
      return;
    const controller = new AbortController();
    requestController = controller;

    try {
      const data = await apiRequest<ImageStatusResponse>(
        `/images/${encodeURIComponent(this.imageId)}/result`,
        { signal: controller.signal },
      );
      if (revision !== requestRevision) return;
      if (
        !data ||
        data.imageId !== this.imageId ||
        !["PENDING", "PROCESSING", "COMPLETED", "FAILED"].includes(data.status)
      ) {
        throw new Error("Invalid verification response");
      }

      runInAction(() => {
        pollFailures = 0;
        if (data.status === "COMPLETED") {
          const probability = data.fakeProbability;
          if (
            (data.predictedClass !== 0 && data.predictedClass !== 1) ||
            typeof probability !== "number" ||
            !Number.isFinite(probability) ||
            probability < 0 ||
            probability > 1
          ) {
            this.phase = "failed";
            this.errorStatus = ErrorType.CommonError;
            return;
          }
          this.result = {
            result: data.predictedClass === 1 ? "Fake" : "Real",
            probability:
              data.predictedClass === 1 ? probability : 1 - probability,
          };
          this.phase = "completed";
          this.errorStatus = null;
        } else if (data.status === "FAILED") {
          this.phase = "failed";
          this.errorStatus =
            data.error === "NO_FACE"
              ? ErrorType.NoFaceDetected
              : ErrorType.CommonError;
        } else {
          this.phase = data.status === "PROCESSING" ? "processing" : "pending";
        }
      });

      if (data.status === "PENDING" || data.status === "PROCESSING") {
        this.schedulePoll(revision);
      }
    } catch (error) {
      if (revision !== requestRevision || controller.signal.aborted) return;
      runInAction(() => {
        if (error instanceof ApiError && error.status === 401) {
          authStore.clearSession();
          this.phase = "failed";
          this.errorStatus = ErrorType.Unauthorized;
          return;
        }
        pollFailures++;
        if (pollFailures >= MAX_POLL_FAILURES) {
          this.phase = "failed";
          this.errorStatus = ErrorType.NetworkError;
        } else {
          this.schedulePoll(revision);
        }
      });
    } finally {
      if (requestController === controller) requestController = null;
    }
  }

  private schedulePoll(revision: number) {
    if (revision !== requestRevision) return;
    if (pollTimer) clearTimeout(pollTimer);
    pollTimer = setTimeout(() => {
      pollTimer = null;
      void this.poll(revision);
    }, POLL_INTERVAL_MS);
  }

  private setRequestError(error: unknown) {
    this.phase = "failed";
    if (error instanceof ApiError) {
      if (error.status === 401) {
        authStore.clearSession();
        this.errorStatus = ErrorType.Unauthorized;
      } else if (error.status === 413) {
        this.errorStatus = ErrorType.FileTooLarge;
      } else if (error.status === 415 || error.status === 400) {
        this.errorStatus = ErrorType.InvalidFile;
      } else {
        this.errorStatus = ErrorType.CommonError;
      }
    } else {
      this.errorStatus = ErrorType.NetworkError;
    }
  }
}

export const validationStore = new ValidationStore();
