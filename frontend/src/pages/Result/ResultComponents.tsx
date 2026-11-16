import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import type { ValidationResult } from "../../store/validation";
import { ErrorType, validationStore } from "../../store/validation";
import { UploadButton } from "../../components/UploadButton/UploadButton";
import Loader from "./loader.svg?react";
import css from "./Result.module.scss";

export const Loading = () => {
  const { t } = useTranslation();

  return (
    <div className={css.loadingIndicator} role="status" aria-live="polite">
      <div className={css.loaderContainer}>
        <Loader />
      </div>
      {t("Result.processing")}
    </div>
  );
};

export const ErrorMessage = ({
  details,
  canRetry,
}: {
  details: ErrorType | null;
  canRetry: boolean;
}) => {
  const { t } = useTranslation();

  const getErrorMessage = () => {
    switch (details) {
      case ErrorType.NoFaceDetected:
        return t("Result.noFaceDetected");
      case ErrorType.InvalidFile:
        return t("Result.invalidFile");
      case ErrorType.FileTooLarge:
        return t("Result.fileTooLarge");
      case ErrorType.NetworkError:
        return t("Result.networkError");
      case ErrorType.Unauthorized:
        return t("Result.sessionExpired");
      default:
        return "";
    }
  };

  return (
    <div className={css.ErrorMessage}>
      <div className={css.warning} role="alert">
        <p className={css.title}>{t("Result.oops")}</p>
        <p className={css.info}>{t("Result.errorMessage")}</p>
        {details && <p>{getErrorMessage()}</p>}
      </div>

      {canRetry && details === ErrorType.NetworkError && (
        <button
          type="button"
          className={css.retryButton}
          onClick={() => validationStore.retry()}
        >
          {t("Result.retryStatus")}
        </button>
      )}

      <UploadButton title={t("Result.retry")} />
    </div>
  );
};

export const ValidationView = ({
  validationData,
  file,
}: {
  validationData: ValidationResult;
  file: File | null;
}) => {
  const { result, probability } = validationData;
  const { t } = useTranslation();
  const isFake = result === "Fake";

  return (
    <div className={css.container}>
      <ImagePreview file={file} isFake={isFake} />

      <div className={css.infoContainer}>
        <h2 className={css.resultTitle}>{t("Result.analysisResult")}</h2>

        <div className={`${css.status} ${isFake ? css.fake : css.real}`}>
          {isFake ? t("Result.isDeepfake") : t("Result.isReal")}
        </div>

        <p className={css.resultComment}>
          {isFake ? t("Result.couldBeDeepfake") : t("Result.looksReal")}
        </p>

        <p className={css.details}>
          {t("Result.probability", {
            p: (probability * 100).toFixed(1),
          })}
        </p>

        <UploadButton title={t("Result.checkOther")} />
      </div>

      <p className={css.description}>{t("Result.aboutDeepfake")}</p>
    </div>
  );
};

export const ImagePreview = ({
  file,
  isFake,
}: {
  file: File | null;
  isFake: boolean;
}) => {
  const [preview, setPreview] = useState<{
    file: File;
    url: string;
  } | null>(null);

  const dialogRef = useRef<HTMLDialogElement>(null);
  const { t } = useTranslation();

  useEffect(() => {
    if (!file) return;

    const reader = new FileReader();

    const handleLoad = () => {
      if (typeof reader.result === "string") {
        setPreview({ file, url: reader.result });
      }
    };

    reader.addEventListener("load", handleLoad);
    reader.readAsDataURL(file);

    return () => {
      reader.removeEventListener("load", handleLoad);

      if (reader.readyState === FileReader.LOADING) {
        reader.abort();
      }
    };
  }, [file]);

  const imageURL = preview?.file === file ? preview.url : null;

  const openPreview = () => {
    dialogRef.current?.showModal();
  };

  return (
    <div className={css.previewColumn}>
      <div
        className={`${css.imageContainer} ${
          isFake ? css.glowFake : css.glowReal
        }`}
      >
        {imageURL ? (
          <button
            type="button"
            className={css.previewImageButton}
            onClick={openPreview}
            aria-label={t("Result.viewFullImage")}
          >
            <img src={imageURL} alt={t("Result.loadedImage")} />
          </button>
        ) : (
          <div className={css.imagePlaceholder}>
            {t("Result.previewUnavailable")}
          </div>
        )}
      </div>

      {imageURL && (
        <>
          <button
            type="button"
            className={css.viewFullButton}
            onClick={openPreview}
          >
            {t("Result.viewFullImage")}
          </button>

          <dialog
            ref={dialogRef}
            className={css.imageDialog}
            aria-label={t("Result.loadedImage")}
          >
            <button
              type="button"
              className={css.closeDialogButton}
              onClick={() => dialogRef.current?.close()}
              aria-label={t("Result.closePreview")}
            >
              ×
            </button>

            <img src={imageURL} alt={t("Result.loadedImage")} />
          </dialog>
        </>
      )}
    </div>
  );
};
