import os
import torch
import torch.nn as nn
from torchvision import transforms, models
from PIL import Image
import pandas as pd
import numpy as np
import joblib
from catboost import CatBoostClassifier

from calc import calculate_metrics_one
from train import get_x

# Пути к моделям
MODEL_PATHS = {
    'catboost_ef': 'models_to_test/catboost_ef.cbm',
    'catboost_fl': 'models_to_test/catboost_fl.cbm',
    'catboost_lbp': 'models_to_test/catboost_lbp.cbm',
    'catboost_tf_sf': 'models_to_test/catboost_tf_sf.cbm',
    'efficientnet': 'models_to_test/efficientnet_model.pth',
    'meta_model': 'stacking_meta_model_catboost.pkl'
}

# Параметры
IMAGE_SIZE = (224, 224)
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")


class ImageClassifier:
    def __init__(self):
        # Загрузка всех моделей
        self.load_models()

        # Преобразования для изображения
        self.transform = transforms.Compose([
            transforms.Resize(IMAGE_SIZE),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                 std=[0.229, 0.224, 0.225]),
        ])

    def load_models(self):
        """Загрузка всех необходимых моделей"""
        # Загрузка CatBoost моделей
        self.catboost_models = {
            'ef': CatBoostClassifier().load_model(MODEL_PATHS['catboost_ef']),
            'fl': CatBoostClassifier().load_model(MODEL_PATHS['catboost_fl']),
            'lbp': CatBoostClassifier().load_model(MODEL_PATHS['catboost_lbp']),
            'tf_sf': CatBoostClassifier().load_model(MODEL_PATHS['catboost_tf_sf'])
        }

        # Загрузка EfficientNet
        self.effnet = models.efficientnet_b0(pretrained=False)
        self.effnet.classifier[1] = nn.Linear(self.effnet.classifier[1].in_features, 2)
        self.effnet.load_state_dict(torch.load(MODEL_PATHS['efficientnet']))
        self.effnet = self.effnet.to(DEVICE)
        self.effnet.eval()

        # Загрузка мета-модели
        self.meta_model = joblib.load(MODEL_PATHS['meta_model'])

    def get_image_features(self, image_path):
        """Получение всех фичей для изображения"""
        # Вычисляем метрики с помощью calculate_metrics_one
        features_df = calculate_metrics_one(image_path)

        # Добавляем путь к файлу для совместимости
        features_df['file'] = image_path

        return features_df

    def get_catboost_predictions(self, features_df):
        """Получение предсказаний от всех CatBoost моделей"""
        # Подготовка фичей для каждой модели
        rf1_features = self.get_rf1_features(features_df)
        rf2_features = self.get_rf2_features(features_df)
        rf3_features = self.get_rf3_features(features_df)
        rf4_features = self.get_rf4_features(features_df)

        # Получение предсказаний
        probs = [
            self.catboost_models['ef'].predict_proba(rf1_features),
            self.catboost_models['fl'].predict_proba(rf2_features),
            self.catboost_models['lbp'].predict_proba(rf3_features),
            self.catboost_models['tf_sf'].predict_proba(rf4_features)
        ]

        # Объединение предсказаний
        return np.hstack(probs)

    def get_efficientnet_predictions(self, image_path):
        """Получение предсказаний от EfficientNet"""
        image = Image.open(image_path).convert('RGB')
        image = self.transform(image).unsqueeze(0).to(DEVICE)

        with torch.no_grad():
            outputs = self.effnet(image)
            probs = torch.softmax(outputs, dim=1).cpu().numpy()

        return probs

    def predict_image(self, image_path):
        """Основная функция для предсказания"""
        # 1. Получаем фичи изображения
        features_df = self.get_image_features(image_path)

        # 2. Получаем предсказания от CatBoost моделей
        cb_probs = self.get_catboost_predictions(features_df)

        # 3. Получаем предсказания от EfficientNet
        effnet_probs = self.get_efficientnet_predictions(image_path)

        # 4. Объединяем предсказания для мета-модели
        stacked_features = np.hstack((effnet_probs, cb_probs))

        # 5. Делаем финальное предсказание
        final_pred = self.meta_model.predict(stacked_features)
        final_prob = self.meta_model.predict_proba(stacked_features)

        return {
            'prediction': 'Fake' if final_pred[0] == 1 else 'Real',
            'probability': float(final_prob[0][1]),  # Вероятность класса "Fake"
            'class': int(final_pred[0])
        }

    # Функции для извлечения фичей (аналогичные вашим)
    def get_rf1_features(self, df):
        X = df.loc[:, 'face_k':'area']
        X = pd.concat([X, df.loc[:, 'eye_distance':'eye_slant_angle']], axis=1)
        X = pd.concat([X, df.loc[:, 'left_eye_aspect_ratio':'avg_eye_aspect_ratio']], axis=1)
        X = pd.concat([X, df.loc[:, 'eye_reflection_ratio_low':'eye_reflection_ratio_high']], axis=1)
        X = pd.concat([X, df.loc[:, 'hair_texture_std':'hair_contrast']], axis=1)
        return X.drop(['hair_color_std', 'hair_color_mean'], axis=1, errors='ignore')

    def get_rf2_features(self, df):
        X = df.loc[:, '0':'477']
        X = get_x(X)
        return X

    def get_rf3_features(self, df):
        return df.loc[:, 'hist_0':'hist_25']

    def get_rf4_features(self, df):
        X = df.loc[:, 'mean_intensity':'median_intensity']
        X = pd.concat([X, df.loc[:, 'contrast':'correlation']], axis=1)
        X = pd.concat([X, df.loc[:, 'skin_tone_std':'edge_density']], axis=1)
        X = pd.concat([X, df.loc[:, 'dct_std':'fft_high_freq_energy']], axis=1)
        return pd.concat([X, df.loc[:, 'avg_face_depth':'depth_ratio']], axis=1)


# Пример использования
if __name__ == "__main__":
    classifier = ImageClassifier()

    # Путь к тестовому изображению
    test_image_path = "test_img/1.jpg"
    #test_image_path = "test_img/1_orig.png"
    #test_image_path = "test_img/2_orig.jpg"
    #test_image_path = "temp_face_0.jpg"

    # Получаем предсказание
    result = classifier.predict_image(test_image_path)

    print("\nPrediction Result:")
    print(f"Image: {os.path.basename(test_image_path)}")
    print(f"Prediction: {result['prediction']}")
    print(f"Probability: {result['probability']:.4f}")
    print(f"Class: {result['class']} (0=Real, 1=Fake)")