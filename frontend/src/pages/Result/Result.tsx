import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { observer } from "mobx-react-lite";
import { Link, useNavigate } from "react-router-dom";
import { Header } from "../../components/Header/Header";
import { Footer } from "../../components/Footer/Footer";
import { validationStore } from "../../store/validation";
import { Loading, ErrorMessage, ValidationView } from "./ResultComponents";
import css from "./Result.module.scss";

export const ResultPage = observer(() => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { file, imageId, result, loading, errorStatus } = validationStore;

  useEffect(() => {
    if (!imageId && !file) {
      navigate("/", { replace: true });
    } else {
      validationStore.resume();
    }
  }, [navigate, imageId, file]);

  const getPageBodyComponent = () => {
    if (loading) return <Loading />;
    if (result) return <ValidationView file={file} validationData={result} />;
    return <ErrorMessage details={errorStatus} canRetry={!!imageId} />;
  };

  return (
    <div className={css.page}>
      <Header alwaysVisible />
      <main className={css.mainContent}>
        <nav className={css.navigation} aria-label={t("Result.backHome")}>
          <Link to="/" className={css.backLink}>
            <span aria-hidden="true">←</span> {t("Result.backHome")}
          </Link>
        </nav>
        {getPageBodyComponent()}
      </main>
      <Footer />
    </div>
  );
});
