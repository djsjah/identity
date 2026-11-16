import { useNavigate } from "react-router-dom";
import { validationStore } from "../../store/validation";
import css from "./UploadButton.module.scss";
import { observer } from "mobx-react-lite";
import { useRef } from "react";
import { authStore } from "../../store/auth";

type Props = {
  title: string;
};

export const UploadButton = observer(({ title }: Props) => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleClick = () => {
    if (!authStore.initialized) return;
    if (!authStore.isAuthenticated) {
      authStore.setAuthModalMode("login");
    } else {
      fileInputRef.current?.click();
    }
  };

  const handleUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    void validationStore.validate(file);
    navigate("/result");
  };

  return (
    <div className={css.uploadWrapper}>
      <button
        className={css.uploadButton}
        onClick={handleClick}
        disabled={!authStore.initialized}
      >
        {title}
      </button>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png"
        onChange={handleUpload}
        aria-label={title}
        hidden
      />
    </div>
  );
});
