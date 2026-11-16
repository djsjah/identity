import os
from os import listdir
from os.path import isfile, join

from skimage.feature import local_binary_pattern, graycoprops, graycomatrix
from skimage.util import img_as_ubyte

from preprocessing.face_feature_extractor_main import FaceFeatureExtractor

METHOD = 'uniform'
radius = 3
n_points = 8 * radius
import pandas as pd
from tqdm import tqdm

import cv2
import mediapipe as mp

import numpy as np
from scipy.spatial import ConvexHull

import ast

mp_face_detection = mp.solutions.face_detection
mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles
mp_face_mesh = mp.solutions.face_mesh

ALL_DIR = "all_to_all"
ALL_RES = "all_to_all_result"

def rgb_to_hex(rgb):
    return '%02x%02x%02x' % rgb

def get_landmark_data(landmarks):
    x_mas = []
    y_mas = []
    z_mas = []
    points_mas = []
    for landmark in landmarks:
        x_mas.append(landmark.x)
        y_mas.append(landmark.y)
        z_mas.append(landmark.z)
        point = [landmark.x, landmark.y, landmark.z]
        points_mas.append(point)
    x = np.asarray(x_mas)
    y = np.asarray(y_mas)
    z = np.asarray(z_mas)
    points = np.asarray(points_mas)
    return x, y, z, points


def get_dist(pt1, pt2):
    dist = pt1 - pt2
    return (dist[0] ** 2 + dist[1] ** 2 + dist[2] ** 2) ** 0.5


def get_distances(points):
    height = get_dist(points[10], points[18])
    width = get_dist(points[34], points[264])
    mouth_h = get_dist(points[12], points[18])
    mouth_w = get_dist(points[57], points[287])
    l_eye_h = get_dist(points[159], points[145])
    l_eye_w = get_dist(points[130], points[133])
    r_eye_h = get_dist(points[385], points[374])
    r_eye_w = get_dist(points[463], points[263])
    l_ear_eye = get_dist(points[34], points[130])
    r_ear_eye = get_dist(points[263], points[264])
    eyes = get_dist(points[133], points[463])
    l_eye_nose = get_dist(points[133], points[1])
    r_eye_nose = get_dist(points[463], points[1])
    l_ear_mouth = get_dist(points[34], points[18])
    r_ear_mouth = get_dist(points[18], points[264])

    l_eye_k = l_eye_h / l_eye_w
    r_eye_k = r_eye_h / r_eye_w

    mouth_k = mouth_h / mouth_w

    k1_l = l_ear_eye / l_eye_nose
    k1_r = r_ear_eye / r_eye_nose

    k2_l = eyes / l_eye_nose
    k2_r = eyes / r_eye_nose

    k3_l = l_ear_eye / l_ear_mouth
    k3_r = r_ear_eye / r_ear_mouth

    face_k = height / width

    return [face_k, l_eye_k, r_eye_k, mouth_k, k1_l, k1_r, k2_l, k2_r, k3_l, k3_r]


def extract_statistical_features(image):
    # Преобразуем изображение в оттенки серого
    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Вычисляем гистограмму интенсивностей
    hist = cv2.calcHist([gray_image], [0], None, [256], [0, 256])

    # Нормализуем гистограмму
    hist /= hist.sum()

    # Вычисляем среднее значение интенсивности
    mean_intensity = np.mean(gray_image)

    # Вычисляем стандартное отклонение интенсивности
    std_intensity = np.std(gray_image)

    # Вычисляем медиану интенсивности
    median_intensity = np.median(gray_image)

    # Возвращаем все признаки в виде словаря
    return {
        'histogram': hist,
        'mean_intensity': mean_intensity,
        'std_intensity': std_intensity,
        'median_intensity': median_intensity
    }


def extract_glcm_features(image, distances=[1], angles=[0, np.pi / 4, np.pi / 2, 3 * np.pi / 4]):
    # Преобразуем изображение в оттенки серого
    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Преобразуем изображение в 8-битный формат
    gray_image = img_as_ubyte(gray_image)

    # Вычисляем GLCM (Gray-Level Co-occurrence Matrix)
    glcm = graycomatrix(gray_image, distances=distances, angles=angles, levels=256, symmetric=True, normed=True)

    # Извлекаем свойства GLCM (контраст, энергия, однородность, корреляция)
    contrast = graycoprops(glcm, 'contrast').mean()
    energy = graycoprops(glcm, 'energy').mean()
    homogeneity = graycoprops(glcm, 'homogeneity').mean()
    correlation = graycoprops(glcm, 'correlation').mean()

    return {
        'contrast': contrast,
        'energy': energy,
        'homogeneity': homogeneity,
        'correlation': correlation
    }

def transform_feature_arr(feature_arr):
    # Создаем словарь для хранения результата
    result = {}

    # Проверяем, что список не пустой
    if not feature_arr:
        return result

    # Получаем список всех ключей (признаков) из первого словаря
    keys = feature_arr[0].keys()

    # Инициализируем словарь с пустыми списками для каждого ключа
    for key in keys:
        result[key] = []

    # Заполняем списки значениями из каждого словаря
    for item in feature_arr:
        for key in keys:
            result[key].append(item[key])

    return result

def calculate_metrics(image_files, out: str, is_fake=False):
    drawing_spec = mp_drawing.DrawingSpec(thickness=1, circle_radius=1)

    data_mas = []
    feature_arr = []

    with mp_face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=True,
            min_detection_confidence=0.5) as face_mesh:
        for idx, file in enumerate(tqdm(image_files)):
            image = cv2.imread(file)
            volume = None
            area = None
            height = None
            width = None
            data_dist = []
            # Convert the BGR image to RGB before processing.
            results = face_mesh.process(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))

            # Print and draw face mesh landmarks on the image.
            if not results.multi_face_landmarks:
                continue
            annotated_image = image.copy()
            for face_landmarks in results.multi_face_landmarks:
                mp_drawing.draw_landmarks(
                    image=annotated_image,
                    landmark_list=face_landmarks,
                    connections=mp_face_mesh.FACEMESH_TESSELATION,
                    landmark_drawing_spec=None,
                    connection_drawing_spec=mp_drawing_styles
                    .get_default_face_mesh_tesselation_style())
                mp_drawing.draw_landmarks(
                    image=annotated_image,
                    landmark_list=face_landmarks,
                    connections=mp_face_mesh.FACEMESH_CONTOURS,
                    landmark_drawing_spec=None,
                    connection_drawing_spec=mp_drawing_styles
                    .get_default_face_mesh_contours_style())
                mp_drawing.draw_landmarks(
                    image=annotated_image,
                    landmark_list=face_landmarks,
                    connections=mp_face_mesh.FACEMESH_IRISES,
                    landmark_drawing_spec=None,
                    connection_drawing_spec=mp_drawing_styles
                    .get_default_face_mesh_iris_connections_style())

                x, y, z, points = get_landmark_data(face_landmarks.landmark)
                get_distances(points)
                hull = ConvexHull(points)
                volume = hull.volume
                area = hull.area
                data_dist = get_distances(points)

            #lbp
            gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            image_ndarray = np.asarray(gray_image)
            img = image_ndarray
            lbp = local_binary_pattern(img, n_points, radius, METHOD)
            hist, _ = np.histogram(lbp.ravel(), bins=np.arange(0, n_points + 3), range=(0, n_points + 2))
            hist = hist.astype("float")
            hist /= (hist.sum() + 1e-6)

            #statistical
            stat_dict = extract_statistical_features(image)

            #texture
            texture_dict = extract_glcm_features(image)

            #hog
            #image_array = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            #image_ndarray = np.asarray(image_array)
            # fd, hog_image = hog(
            #     image_ndarray,
            #     orientations=8,
            #     pixels_per_cell=(16, 16),
            #     cells_per_block=(1, 1),
            #     visualize=True,
            #     channel_axis=-1,
            # )
            # print(hog_image.shape)

            data = data_dist
            data.append(volume)
            data.append(area)
            data.append(file)
            data.append(is_fake)

            feat_extr = FaceFeatureExtractor()
            try:
                features = feat_extr.extract_features(image)
                if features != None:
                    features = features[0]
            except Exception as e:
                features = None

            feature_arr.append(features)

            data.append(stat_dict['mean_intensity'])
            data.append(stat_dict['std_intensity'])
            data.append(stat_dict['median_intensity'])

            for key in feature_arr[0].keys():
                if features is None:
                    data.append(-1)
                else:
                    data.append(features[key])

            data.append(texture_dict['contrast'])
            data.append(texture_dict['energy'])
            data.append(texture_dict['homogeneity'])
            data.append(texture_dict['correlation'])
            data = data + hist.tolist()
            data = data + points.tolist()
            data_mas.append(data)

    keys_feature_dict = feature_arr[0].keys()

    column_labels = [str(i) for i in range(478)]
    hist_labels = ['hist_' + str(i) for i in range(n_points + 2)]
    columns = (['face_k', 'l_eye_k', 'r_eye_k', 'mouth_k',
               'k1_l', 'k1_r', 'k2_l', 'k2_r', 'k3_l', 'k3_r',
               'volume', 'area', 'file', 'is_fake',
               'mean_intensity', 'std_intensity', 'median_intensity']
               + list(keys_feature_dict)
               + ['contrast', 'energy', 'homogeneity', 'correlation'])
    columns = columns + hist_labels + column_labels
    df = pd.DataFrame(data_mas, columns=columns)
    df.to_csv(out)
    # print(volume, area, height, width, height / width, points)


def calculate_metrics_one(image_file):
    drawing_spec = mp_drawing.DrawingSpec(thickness=1, circle_radius=1)

    data_mas = []
    feature_arr = []

    with mp_face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=True,
            min_detection_confidence=0.5) as face_mesh:

        image = cv2.imread(image_file)
        volume = None
        area = None
        height = None
        width = None
        data_dist = []
        # Convert the BGR image to RGB before processing.
        results = face_mesh.process(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))

        # Print and draw face mesh landmarks on the image.
        if not results.multi_face_landmarks:
            return []

        annotated_image = image.copy()
        for face_landmarks in results.multi_face_landmarks:
            mp_drawing.draw_landmarks(
                image=annotated_image,
                landmark_list=face_landmarks,
                connections=mp_face_mesh.FACEMESH_TESSELATION,
                landmark_drawing_spec=None,
                connection_drawing_spec=mp_drawing_styles
                .get_default_face_mesh_tesselation_style())
            mp_drawing.draw_landmarks(
                image=annotated_image,
                landmark_list=face_landmarks,
                connections=mp_face_mesh.FACEMESH_CONTOURS,
                landmark_drawing_spec=None,
                connection_drawing_spec=mp_drawing_styles
                .get_default_face_mesh_contours_style())
            mp_drawing.draw_landmarks(
                image=annotated_image,
                landmark_list=face_landmarks,
                connections=mp_face_mesh.FACEMESH_IRISES,
                landmark_drawing_spec=None,
                connection_drawing_spec=mp_drawing_styles
                .get_default_face_mesh_iris_connections_style())

            x, y, z, points = get_landmark_data(face_landmarks.landmark)
            get_distances(points)
            hull = ConvexHull(points)
            volume = hull.volume
            area = hull.area
            data_dist = get_distances(points)

        #lbp
        gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        image_ndarray = np.asarray(gray_image)
        img = image_ndarray
        lbp = local_binary_pattern(img, n_points, radius, METHOD)
        hist, _ = np.histogram(lbp.ravel(), bins=np.arange(0, n_points + 3), range=(0, n_points + 2))
        hist = hist.astype("float")
        hist /= (hist.sum() + 1e-6)

        #statistical
        stat_dict = extract_statistical_features(image)

        #texture
        texture_dict = extract_glcm_features(image)

        #hog
        #image_array = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        #image_ndarray = np.asarray(image_array)
        # fd, hog_image = hog(
        #     image_ndarray,
        #     orientations=8,
        #     pixels_per_cell=(16, 16),
        #     cells_per_block=(1, 1),
        #     visualize=True,
        #     channel_axis=-1,
        # )
        # print(hog_image.shape)

        data = data_dist
        data.append(volume)
        data.append(area)
        data.append(image_file)

        feat_extr = FaceFeatureExtractor()
        try:
            features = feat_extr.extract_features(image)
            if features != None:
                features = features[0]
        except Exception as e:
            features = None

        feature_arr.append(features)

        data.append(stat_dict['mean_intensity'])
        data.append(stat_dict['std_intensity'])
        data.append(stat_dict['median_intensity'])

        for key in feature_arr[0].keys():
            if features is None:
                data.append(-1)
            else:
                data.append(features[key])

        data.append(texture_dict['contrast'])
        data.append(texture_dict['energy'])
        data.append(texture_dict['homogeneity'])
        data.append(texture_dict['correlation'])
        data = data + hist.tolist()
        data = data + points.tolist()
        data_mas.append(data)

    keys_feature_dict = feature_arr[0].keys()

    column_labels = [str(i) for i in range(478)]
    hist_labels = ['hist_' + str(i) for i in range(n_points + 2)]
    columns = (['face_k', 'l_eye_k', 'r_eye_k', 'mouth_k',
               'k1_l', 'k1_r', 'k2_l', 'k2_r', 'k3_l', 'k3_r',
               'volume', 'area', 'file',
               'mean_intensity', 'std_intensity', 'median_intensity']
               + list(keys_feature_dict)
               + ['contrast', 'energy', 'homogeneity', 'correlation'])
    columns = columns + hist_labels + column_labels
    df = pd.DataFrame(data_mas, columns=columns)
    df = df.drop('hair_color_std', axis=1)
    df = df.drop('hair_color_mean', axis=1)
    df.to_csv('tmp_single.csv')
    df = pd.read_csv('tmp_single.csv')
    df = clear_unnamed(df)
    return df

def get_all_files(path):
    return [path + "\\" + f for f in listdir(path) if isfile(join(path, f))]

def clear_unnamed(df):
    df = df.loc[:, ~df.columns.str.contains('^Unnamed')]
    return df


def get_x_new(X):
    file = "celeba_x_new.csv"
    if os.path.exists(file):
        X_new = pd.read_csv(file)
        X_new = clear_unnamed(X_new)
        return X_new

    X_new = pd.DataFrame()
    column_labels = [str(i) for i in range(478)]  # 478
    for column_name in tqdm(column_labels):
        x = []
        y = []
        z = []
        X[column_name] = X[column_name].apply(lambda x: ast.literal_eval(x))
        for vec in X[column_name]:
            x.append(vec[0])
            y.append(vec[1])
            z.append(vec[2])
        X_new[column_name + "x"] = x
        X_new[column_name + "y"] = y
        X_new[column_name + "z"] = z

    X_new.to_csv(file)
    return X_new


def get_x_new_2(X):
    file = "celeba_x_new_2.csv"
    if os.path.exists(file):
        X_new = pd.read_csv(file)
        X_new = clear_unnamed(X_new)
        return X_new

    X_new = pd.DataFrame()
    column_labels = [str(i) for i in range(478)]  # 478
    for column_name in tqdm(column_labels):
        x = []
        y = []
        z = []
        X[column_name] = X[column_name].apply(lambda x: ast.literal_eval(x))
        for vec in X[column_name]:
            x.append(vec[0])
            y.append(vec[1])
            z.append(vec[2])
        X_new[column_name + "x"] = x
        X_new[column_name + "y"] = y
        X_new[column_name + "z"] = z

    X_new.to_csv(file)
    return X_new

def join_csv(csv1, csv2, res):
    df1 = pd.read_csv(csv1)
    df2 = pd.read_csv(csv2)
    df_merged = pd.concat([df1, df2], ignore_index=True)
    df_merged = clear_unnamed(df_merged)
    df_merged.to_csv(res)

