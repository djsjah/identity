import "./StarsContainer.scss";

export const StarsContainer = () => {
  return (
    <div className="star-container" aria-hidden="true">
      <div className="stars starsSmall" />
      <div className="stars starsMedium" />
      <div className="stars starsLarge" />
    </div>
  );
};
