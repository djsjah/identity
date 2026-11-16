import css from "./Landing.module.scss";
import { Header } from "../../components/Header/Header";
import { UploadButton } from "../../components/UploadButton/UploadButton";
import { Features } from "../../components/Features/Features";
import { StarsContainer } from "../../components/StarsContainer/StarsContainer";
import { useRef } from "react";
import { useTranslation } from "react-i18next";
import PipelineSection from "../../components/Pipeline/Pipeline";
import { Footer } from "../../components/Footer/Footer";

export const Landing = () => {
  const heroSection = useRef<HTMLDivElement>(null);
  const { t } = useTranslation();

  return (
    <div className={css.landing}>
      <Header alwaysVisible={false} dependencyBlock={heroSection} />
      <section className={css.hero} ref={heroSection}>
        <StarsContainer />
        <div className={css.title}>
          <h1>I-Dentity</h1>
          <p>{t("Landing.tagline")}</p>
        </div>
        <div className={css.uploadWrapper}>
          <UploadButton title={t("Landing.uploadImage")} />
        </div>
      </section>
      <section className={css.about}>
        <h2>{t("Landing.aboutUsTitle")}</h2>
        <p>{t("Landing.aboutUsContent")}</p>
      </section>
      <PipelineSection />
      <Features />
      <Footer withStars />
    </div>
  );
};
