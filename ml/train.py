import os
import warnings
from os import listdir
from os.path import isfile, join

import pickle

import pandas as pd
from tqdm import tqdm

import cv2
import mediapipe as mp

import numpy as np
import matplotlib.pyplot as plt
from scipy.spatial import ConvexHull


from sklearn import svm
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import Perceptron
from sklearn.model_selection import train_test_split
from sklearn import metrics
from sklearn.metrics import accuracy_score, f1_score
from sklearn.metrics import confusion_matrix
import joblib

from catboost import CatBoostClassifier

import ast

mp_face_detection = mp.solutions.face_detection
mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles
mp_face_mesh = mp.solutions.face_mesh

ALL_DIR = "all_to_all"
ALL_RES = "all_to_all_result"

def mark(image_files, out_dir):
    with mp_face_detection.FaceDetection(
            model_selection=1, min_detection_confidence=0.5) as face_detection:
        for idx, file in enumerate(image_files):
            image = cv2.imread(file)
            # Convert the BGR image to RGB and process it with MediaPipe Face Detection.
            results = face_detection.process(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))

            # Draw face detections of each face.
            if not results.detections:
                continue
            annotated_image = image.copy()
            for detection in results.detections:
                print('Nose tip:')
                print(mp_face_detection.get_key_point(
                    detection, mp_face_detection.FaceKeyPoint.NOSE_TIP))
                mp_drawing.draw_detection(annotated_image, detection)
            cv2.imwrite(f'{out_dir}/' + str(idx) + '.jpg', annotated_image)


def drawSimpl(hull, points):
    fig = plt.figure()
    ax = fig.add_subplot(111, projection="3d")

    # Plot defining corner points
    ax.plot(points.T[0], points.T[1], points.T[2], "ko")

    # 12 = 2 * 6 faces are the simplices (2 simplices per square face)
    for s in hull.simplices:
        s = np.append(s, s[0])  # Here we cycle back to the first coordinate
        ax.plot(points[s, 0], points[s, 1], points[s, 2], "r-")

    # Make axis label
    for i in ["x", "y", "z"]:
        eval("ax.set_{:s}label('{:s}')".format(i, i))

    plt.show()


def rgb_to_hex(rgb):
    return '%02x%02x%02x' % rgb


def drawXYZ(x, y, z):
    with open("coeffs_points", "rb") as fp:  # Unpickling
        coeffs = pickle.load(fp)

    coeffs = np.array(coeffs)
    coeffs = (coeffs - coeffs.min()) / (coeffs.max() - coeffs.min())

    labels = list(range(468))
    fig = plt.figure()
    ax = plt.axes(projection='3d')

    print(len(x))
    counter = 0
    for xyz, label in zip(zip(x, y, z), coeffs):
        # if counter in coeffs_best:
        #     label = 1
        #     color = rgb_to_hex((int((255 * label) // 1), int((255 * (1 - label)) // 1), 0))
        #     ax.scatter(*xyz, color=f"#{color}")
        # else:
        #     label = 0
        #
        # counter += 1
        size = label * 50
        color = rgb_to_hex((int((255 * label) // 1), int((255 * (1 - label)) // 1), 0))
        ax.scatter(*xyz, color=f"#{color}", s=size)

    c = x + y
    # ax.scatter(x, y, z, c=c)
    # plotting
    # ax.plot3D(x, y, z, 'green')
    # Hide grid lines
    ax.grid(False)

    # Hide axes ticks
    ax.set_xticks([])
    ax.set_yticks([])
    ax.set_zticks([])
    plt.show()


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


def mark_mesh(image_files, out_dir):
    drawing_spec = mp_drawing.DrawingSpec(thickness=1, circle_radius=1)
    with mp_face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=True,
            min_detection_confidence=0.5) as face_mesh:
        for idx, file in enumerate(image_files):
            image = cv2.imread(file)
            volume = None
            area = None
            height = None
            width = None
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
                # height, width = get_distances(points)
                # drawSimpl(hull, points)
                drawXYZ(x, y, z)
            #print(volume, area, height, width, height / width)
            cv2.imwrite(f'{out_dir}/' + str(idx) + '.png', annotated_image)


def compare():
    origins = [ALL_DIR + "/" + f for f in listdir(ALL_DIR) if isfile(join(ALL_DIR, f))]
    fakes = [ALL_RES + "/" + f for f in listdir(ALL_RES) if isfile(join(ALL_RES, f))]
    # mark(origins, "all_to_all_matched")
    # mark(fakes, "all_to_all_res_matched")
    mark_mesh(origins, "mesh")
    mark_mesh(fakes, "mesh_res")


def test():
    photo = ["test" + "/" + f for f in listdir("test") if isfile(join("test", f))]
    print(photo)
    mark_mesh(photo, "test_out")


def calculate_metrics(image_files, out: str, is_fake=False):
    drawing_spec = mp_drawing.DrawingSpec(thickness=1, circle_radius=1)

    data_mas = []

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

            data = data_dist
            data.append(volume)
            data.append(area)
            data.append(file)
            data.append(is_fake)
            data = data + points.tolist()
            data_mas.append(data)

    column_labels = [str(i) for i in range(478)]
    columns = ['face_k', 'l_eye_k', 'r_eye_k', 'mouth_k',
               'k1_l', 'k1_r', 'k2_l', 'k2_r', 'k3_l', 'k3_r',
               'volume', 'area', 'file', 'is_fake']
    columns = columns + column_labels
    df = pd.DataFrame(data_mas, columns=columns)
    df.to_csv(out)
    # print(volume, area, height, width, height / width, points)


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

def train_points(X, y, max_iter=10000, eta0=0.1, early_stopping=False, alpha=0.0001, penalty=None, show_coefs=False):
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)
    clf = Perceptron(max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)

    all = len(y_pred)
    fakes = 0
    for y in y_pred:
        if y:
            fakes += 1

    if show_coefs:
        coeffs = clf.coef_[0]
        print(coeffs)

        coef_res = []
        for i in range(len(coeffs) // 3):
            coef_res.append(abs(coeffs[3 * i]) + abs(coeffs[3 * i + 1]) + abs(coeffs[3 * i + 2]))

        with open("coeffs_points", "wb") as fp:  # Pickling
            pickle.dump(coef_res, fp)

        print(all, fakes)
        print("Accuracy:", accuracy)
        print("F1-score:", f1)
        conf_matrix = confusion_matrix(y_test, y_pred)
        cm_display = metrics.ConfusionMatrixDisplay(confusion_matrix=conf_matrix, display_labels=[False, True])

        cm_display.plot()
        plt.show()
    return accuracy, f1, fakes


def train_metrics(X, y, max_iter=10000, eta0=0.1, early_stopping=False, alpha=0.0001, penalty=None, show_coefs=False):
    # scaler = StandardScaler()
    # scaler.fit(X)
    # X = scaler.transform(X)

    X_new = (X - X.mean()) / X.std()

    X_train, X_test, y_train, y_test = train_test_split(X_new, y, test_size=0.3, random_state=42)
    clf = Perceptron(max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)

    all = len(y_pred)
    fakes = 0
    for y in y_pred:
        if y:
            fakes += 1

    print(all, fakes)

    if show_coefs:
        coefs = clf.coef_[0]
        names = X.columns

        x = zip(coefs, names)
        xs = sorted(x, key=lambda tup: abs(tup[0]), reverse=True)

        a1 = [x[0] for x in xs]
        b1 = [x[1] for x in xs]

        for i in range(len(a1)):
            print(a1[i], b1[i])

        conf_matrix = confusion_matrix(y_test, y_pred)
        cm_display = metrics.ConfusionMatrixDisplay(confusion_matrix=conf_matrix, display_labels=[False, True])

        cm_display.plot()
        plt.show()

    print("Accuracy:", accuracy)
    print("F1-score:", f1)

    return accuracy, f1, fakes


def hyper_params_points(checkpoint=None, best_points = False):
    max_iters = [1000, 10000, 100000, 1000000, 10000000]
    eta0s = [0.01, 0.1, 1, 10, 100]
    early_stoppings = [True, False]
    alphas = [0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001, 0.000000001]
    penaltys = ['l2', 'l1', 'elasticnet', None]

    columns = ['max_iter', 'eta0', 'early_stopping', 'alpha', 'penalty', 'accuracy', 'f1', 'fake_detected']
    data = []

    if checkpoint in max_iters:
        # max_iters.remove(checkpoint)
        data = pd.read_csv(f'res_points_{checkpoint}.csv')
        data = clear_unnamed(data)
        data = data.values.tolist()

    with open("coeffs_points_best", "rb") as fp:  # Pickling
        coeffs_best = pickle.load(fp)
        str_coeffs_best = map(str, coeffs_best)
        print(coeffs_best)

    df = pd.read_csv("celeba_dataset.csv")
    X = df.loc[:, df.columns != 'is_fake']
    if not best_points:
        X = X.loc[:, '0':'477']
    else:
        X = X.loc[:, str_coeffs_best]
    X = get_x_new(X)
    y = df['is_fake']

    for max_iter in max_iters:
        for eta0 in eta0s:
            for early_stopping in early_stoppings:
                for alpha in alphas:
                    for penalty in penaltys:
                        accuracy, f1, fakes = train_points(X, y, max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
                        print(accuracy, f1, fakes)
                        data.append([max_iter, eta0, early_stopping, alpha, penalty, accuracy, f1, fakes])
        df = pd.DataFrame(data, columns=columns)
        index = max_iter
        if checkpoint:
            index = "checkpoint"
        df.to_csv(f'res_points_{index}_{best_points}.csv')


def hyper_params_points_2(checkpoint=None, best_points=False):
    max_iters = [1000, 10000, 100000, 1000000, 10000000]
    eta0s = [0.01, 0.1, 1, 10, 100]
    early_stoppings = [True, False]
    alphas = [0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001, 0.000000001]
    penaltys = ['l2', 'l1', 'elasticnet', None]

    columns = ['max_iter', 'eta0', 'early_stopping', 'alpha', 'penalty', 'accuracy', 'f1', 'fake_detected']
    data = []

    if checkpoint in max_iters:
        # max_iters.remove(checkpoint)
        data = pd.read_csv(f'res_points_{checkpoint}_2.csv')
        data = clear_unnamed(data)
        data = data.values.tolist()

    # with open("coeffs_points_best_2", "rb") as fp:  # Pickling
    #     coeffs_best = pickle.load(fp)
    #     str_coeffs_best = map(str, coeffs_best)
    #     print(coeffs_best)

    df = pd.read_csv("celeba_dataset_2.csv")
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']
    # if not best_points:
    #     X = X.loc[:, '0':'477']
    # else:
    #     X = X.loc[:, str_coeffs_best]
    X = get_x_new_2(X)
    y = df['is_fake']

    print(X.shape, y.shape)

    for max_iter in max_iters:
        for eta0 in eta0s:
            for early_stopping in early_stoppings:
                for alpha in alphas:
                    for penalty in penaltys:
                        accuracy, f1, fakes = train_points(X, y, max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
                        print(accuracy, f1, fakes)
                        data.append([max_iter, eta0, early_stopping, alpha, penalty, accuracy, f1, fakes])
        df = pd.DataFrame(data, columns=columns)
        index = max_iter
        if checkpoint:
            index = "checkpoint"
        df.to_csv(f'res_points_{index}_{best_points}_2.csv')


def hyper_params_all_2(checkpoint=None, best_points=False):
    max_iters = [1000, 10000, 100000, 1000000, 10000000]
    eta0s = [0.01, 0.1, 1, 10, 100]
    early_stoppings = [True, False]
    alphas = [0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001, 0.000000001]
    penaltys = ['l2', 'l1', 'elasticnet', None]

    columns = ['max_iter', 'eta0', 'early_stopping', 'alpha', 'penalty', 'accuracy', 'f1', 'fake_detected']
    data = []

    if checkpoint in max_iters:
        # max_iters.remove(checkpoint)
        data = pd.read_csv(f'res_points_{checkpoint}_all_2.csv')
        data = clear_unnamed(data)
        data = data.values.tolist()

    # with open("coeffs_points_best_2", "rb") as fp:  # Pickling
    #     coeffs_best = pickle.load(fp)
    #     str_coeffs_best = map(str, coeffs_best)
    #     print(coeffs_best)

    df = pd.read_csv("celeba_dataset_2.csv")
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']
    # if not best_points:
    #     X = X.loc[:, '0':'477']
    # else:
    #     X = X.loc[:, str_coeffs_best]
    X = get_x_new_2(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    y = df['is_fake']

    print(X.shape, y.shape)

    for max_iter in max_iters:
        for eta0 in eta0s:
            for early_stopping in early_stoppings:
                for alpha in alphas:
                    for penalty in penaltys:
                        accuracy, f1, fakes = train_points(X, y, max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
                        print(accuracy, f1, fakes)
                        data.append([max_iter, eta0, early_stopping, alpha, penalty, accuracy, f1, fakes])
        df = pd.DataFrame(data, columns=columns)
        index = max_iter
        if checkpoint:
            index = "checkpoint"
        df.to_csv(f'res_points_{index}_{best_points}_all_2.csv')

def hyper_params_metrics(checkpoint=None):
    max_iters = [1000, 10000, 100000, 1000000, 10000000]
    eta0s = [0.01, 0.1, 1, 10, 100]
    early_stoppings = [True, False]
    alphas = [0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001, 0.000000001]
    penaltys = ['l2', 'l1', 'elasticnet', None]

    columns = ['max_iter', 'eta0', 'early_stopping', 'alpha', 'penalty', 'accuracy', 'f1', 'fake_detected']
    data = []

    if checkpoint in max_iters:
        # max_iters.remove(checkpoint)
        data = pd.read_csv(f'res_metrics_{checkpoint}.csv')
        data = clear_unnamed(data)
        data = data.values.tolist()

    df = pd.read_csv("celeba_dataset.csv")
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, 'face_k':'area']

    y = df['is_fake']

    for max_iter in max_iters:
        for eta0 in eta0s:
            for early_stopping in early_stoppings:
                for alpha in alphas:
                    for penalty in penaltys:
                        accuracy, f1, fakes = train_metrics(X, y, max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
                        data.append([max_iter, eta0, early_stopping, alpha, penalty, accuracy, f1, fakes])
        df = pd.DataFrame(data, columns=columns)
        index = max_iter
        if checkpoint:
            index = "checkpoint"
        df.to_csv(f'res_metrics_{index}.csv')


def hyper_params_metrics_2(checkpoint=None):
    max_iters = [1000, 10000, 100000, 1000000, 10000000]
    eta0s = [0.01, 0.1, 1, 10, 100]
    early_stoppings = [True, False]
    alphas = [0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001, 0.000000001]
    penaltys = ['l2', 'l1', 'elasticnet', None]

    columns = ['max_iter', 'eta0', 'early_stopping', 'alpha', 'penalty', 'accuracy', 'f1', 'fake_detected']
    data = []

    if checkpoint in max_iters:
        # max_iters.remove(checkpoint)
        data = pd.read_csv(f'res_metrics_{checkpoint}_2.csv')
        data = clear_unnamed(data)
        data = data.values.tolist()

    df = pd.read_csv("celeba_dataset_2.csv")
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, 'face_k':'area']

    y = df['is_fake']

    for max_iter in max_iters:
        for eta0 in eta0s:
            for early_stopping in early_stoppings:
                for alpha in alphas:
                    for penalty in penaltys:
                        accuracy, f1, fakes = train_metrics(X, y, max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
                        data.append([max_iter, eta0, early_stopping, alpha, penalty, accuracy, f1, fakes])
        df = pd.DataFrame(data, columns=columns)
        index = max_iter
        if checkpoint:
            index = "checkpoint"
        df.to_csv(f'res_metrics_{index}_2.csv')


def draw_points():
    df = pd.read_csv("celeba_dataset.csv")
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']
    X = get_x_new(X)
    y = df['is_fake']
    # accuracy, f1, fakes = train_points(X, y, max_iter=1000, eta0=1, early_stopping=False, alpha=1e-6,
    #                                    penalty='elasticnet', show_coefs=True)
    accuracy, f1, fakes = train_points(X, y, max_iter=1000, eta0=0.01, early_stopping=False, alpha=0.01,
                                       penalty=None, show_coefs=True)
    mark_mesh(["D:\\FaceSwap\\test\\3_to_1.jpg"], "test_out")


def draw_points_best():
    with open("coeffs_points_best", "rb") as fp:
        coeffs_best = pickle.load(fp)
        str_coeffs_best = map(str, coeffs_best)

    df = pd.read_csv("celeba_dataset.csv")
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, str_coeffs_best]
    X = get_x_new(X)
    y = df['is_fake']

    accuracy, f1, fakes = train_points(X, y, max_iter=1000, eta0=0.01, early_stopping=False, alpha=0.01,
                                       penalty=None, show_coefs=True)
    print(accuracy, f1, fakes)


def show_metrics():
    with open("coeffs_points_best", "rb") as fp:
        coeffs_best = pickle.load(fp)
        str_coeffs_best = map(str, coeffs_best)

    df = pd.read_csv("celeba_dataset.csv")
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, 'face_k':'area']

    y = df['is_fake']
    accuracy, f1, fakes = train_metrics(X, y, max_iter=1000, eta0=0.1, early_stopping=False, alpha=1e-8, penalty='l1', show_coefs=True)


def show_all_best():
    with open("coeffs_points_best", "rb") as fp:
        coeffs_best = pickle.load(fp)
        str_coeffs_best = map(str, coeffs_best)

    df = pd.read_csv("celeba_dataset.csv")
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, 'face_k':'area']

    X_new = df.loc[:, str_coeffs_best]
    X_new = get_x_new(X_new)

    X = pd.concat([X, X_new], axis=1)

    y = df['is_fake']
    accuracy, f1, fakes = train_metrics(X, y, max_iter=1000, eta0=0.1, early_stopping=False, alpha=1e-8, penalty='l1', show_coefs=True)


def show_all(dataset_name="celeba_dataset.csv"):
    df = pd.read_csv(dataset_name)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, 'face_k':'area']

    X_new = get_x_new(X)

    X = pd.concat([X, X_new], axis=1)

    y = df['is_fake']
    accuracy, f1, fakes = train_metrics(X, y, max_iter=1000, eta0=0.1, early_stopping=False, alpha=1e-8, penalty='l1', show_coefs=True)


def get_best_features_points():
    with open("coeffs_points", "rb") as fp:
        coeffs = pickle.load(fp)
    coeffs = np.array(coeffs)
    coeffs = (coeffs - coeffs.min()) / (coeffs.max() - coeffs.min())

    indexes = []
    for i in range(len(coeffs)):
        if coeffs[i] > 0.3:
            indexes.append(i)

    with open("coeffs_points_best", "wb") as fp:  # Pickling
        pickle.dump(indexes, fp)


def join_csv(csv1, csv2, res):
    df1 = pd.read_csv(csv1)
    df2 = pd.read_csv(csv2)
    df_merged = pd.concat([df1, df2], ignore_index=True)
    df_merged = clear_unnamed(df_merged)
    df_merged.to_csv(res)


def get_equal_count(csv1, csv2, res):
    df1 = pd.read_csv(csv1)
    df2 = pd.read_csv(csv2)
    df_res = df2.sample(n=df1.shape[0])
    df_res = clear_unnamed(df_res)
    df_res.to_csv(res)

import subprocess

def get_x(X):
    warnings.simplefilter(action='ignore', category=pd.errors.PerformanceWarning)
    file = "tmp_my.csv"
    if os.path.exists(file) and False:
        X_new = pd.read_csv(file)
        X_new = clear_unnamed(X_new)
        return X_new

    X_new = pd.DataFrame()
    column_labels = [str(i) for i in range(478)]  # 478
    #for column_name in tqdm(column_labels):
    for column_name in column_labels:
        x = []
        y = []
        z = []
        X = X.copy()
        X[column_name] = X[column_name].apply(lambda x: ast.literal_eval(x))
        for vec in X[column_name]:
            x.append(vec[0])
            y.append(vec[1])
            z.append(vec[2])
        X_new[column_name + "x"] = x
        X_new[column_name + "y"] = y
        X_new[column_name + "z"] = z

    #X_new.to_csv(file)
    return X_new

def hyper_params_all(path, name=None):
    max_iters = [1000, 10000, 100000, 1000000, 10000000]
    eta0s = [0.01, 0.1, 1, 10, 100]
    early_stoppings = [True, False]
    alphas = [0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001, 0.000000001]
    penaltys = ['l2', 'l1', 'elasticnet', None]

    columns = ['max_iter', 'eta0', 'early_stopping', 'alpha', 'penalty', 'accuracy', 'f1', 'fake_detected']
    data = []

    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    #X = pd.concat([X, df.loc[:, 'lbp':'hog']], axis=1)
    y = df['is_fake']

    print(X.shape, y.shape)

    for max_iter in max_iters:
        for eta0 in eta0s:
            for early_stopping in early_stoppings:
                for alpha in alphas:
                    for penalty in penaltys:
                        accuracy, f1, fakes = train_points(X, y, max_iter=max_iter, eta0=eta0, early_stopping=early_stopping, alpha=alpha, penalty=penalty)
                        print(accuracy, f1, fakes)
                        data.append([max_iter, eta0, early_stopping, alpha, penalty, accuracy, f1, fakes])
        df = pd.DataFrame(data, columns=columns)
        index = max_iter
        if name:
            index = "checkpoint"
        df.to_csv(f'C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\logs\\res_points_{index}_{max_iter}_all.csv')

def random_forest(path, name=None):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, 20)
    print(accuracy, f1, fakes)

def random_forest_EF_FL_LBP(path, depth = 20):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    X = pd.concat([X, df.loc[:, 'hist_0':'hist_25']], axis=1)
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, depth)
    print(accuracy, f1, fakes)
    return accuracy, f1, fakes

def random_forest_EF_FL_LBP_TF_SF(path, depth = 20, is_save=False):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    X = pd.concat([X, df.loc[:, 'mean_intensity':'median_intensity']], axis=1)
    X = pd.concat([X, df.loc[:, 'contrast':'correlation']], axis=1)
    X = pd.concat([X, df.loc[:, 'hist_0':'hist_25']], axis=1)
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, depth, is_save)
    print(accuracy, f1, fakes)
    return accuracy, f1, fakes

def random_forest_all(path, depth = 20, is_save=False):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    X = pd.concat([X, df.loc[:, 'mean_intensity':'correlation']], axis=1)
    X = pd.concat([X, df.loc[:, 'hist_0':'hist_25']], axis=1)
    X = X.drop('hair_color_std', axis=1)
    X = X.drop('hair_color_mean', axis=1)
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, depth, is_save)
    print(accuracy, f1, fakes)
    return accuracy, f1, fakes

def random_forest_FL_LBP(path, name=None):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'hist_0':'hist_25']], axis=1)
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, 20)
    print(accuracy, f1, fakes)

def random_forest_EF(path, save=False):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, 'face_k':'area']
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, 20, save)
    print(accuracy, f1, fakes)


def train_forest(X, y, depth, save = False, name = "model.pth"):
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)
    clf = RandomForestClassifier(max_depth=depth, random_state=0)
    clf.fit(X_train, y_train)
    print(clf.score(X_test, y_test))
    print(clf.feature_importances_)

    y_pred = clf.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    fakes = 0
    for y in y_pred:
        if y:
            fakes += 1

    if save:
        coeffs = clf.feature_importances_
        print(coeffs)

        coef_res = []
        for i in range(len(coeffs) // 3):
            coef_res.append(abs(coeffs[3 * i]) + abs(coeffs[3 * i + 1]) + abs(coeffs[3 * i + 2]))

        with open("coeffs_points", "wb") as fp:  # Pickling
            pickle.dump(coef_res, fp)

        model_filename = f'models_to_test/{name}.pkl'
        joblib.dump(clf, model_filename)
        print(f"Модель сохранена в файл {model_filename}")

    return accuracy, f1, fakes


def train_catboost(X, y, depth, save=False, name="model_catboost.pth"):
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)

    # Инициализация модели CatBoost
    model = CatBoostClassifier(
        depth=depth,
        random_state=42,
        verbose=0,  # Убираем вывод обучения
        loss_function='Logloss'
    )

    # Обучение модели
    model.fit(X_train, y_train)

    # Оценка модели
    print(model.score(X_test, y_test))

    # Получение важности признаков
    feature_importances = model.get_feature_importance()
    print(feature_importances)

    y_pred = model.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    fakes = sum(y_pred)  # Более компактный способ подсчета

    if save:
        coeffs = feature_importances
        print(coeffs)

        # Аналогичная обработка коэффициентов, как в исходной функции
        coef_res = []
        for i in range(len(coeffs) // 3):
            coef_res.append(abs(coeffs[3 * i]) + abs(coeffs[3 * i + 1]) + abs(coeffs[3 * i + 2]))

        with open("coeffs_points_catboost", "wb") as fp:
            pickle.dump(coef_res, fp)

        model_filename = f'models_to_test/{name}.cbm'
        # CatBoost имеет свой метод для сохранения
        model.save_model(model_filename)
        print(f"Модель CatBoost сохранена в файл {model_filename}")

    return accuracy, f1, fakes


def train_log_reg(X, y):
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)
    clf = LogisticRegression(random_state=0)
    clf.fit(X_train, y_train)
    print(clf.score(X_test, y_test))
    print(clf.feature_importances_)
    y_pred = clf.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    fakes = 0
    for y in y_pred:
        if y:
            fakes += 1
    return accuracy, f1, fakes

def hyper_log_reg(path, name=None):
    df = pd.read_csv(path)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    y = df['is_fake']
    accuracy, f1, fakes = train_log_reg(X, y)
    print(accuracy, f1, fakes)

def hyper_forest(path, name=None):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    y = df['is_fake']

    depths = [5, 10, 15, 20, 25, 30, 50]
    data = []
    columns = ['depth', 'accuracy', 'f1', 'fake_detected']

    for depth in depths:
        accuracy, f1, fakes = train_forest(X, y, depth)
        print(accuracy, f1, fakes)
        data.append([depth, accuracy, f1, fakes])

    df = pd.DataFrame(data, columns=columns)
    index = path.split('/')[-1].split('.')[0]
    # os.remove('tmp_my.csv')
    df.to_csv(f'C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\logs\\forest_all_2.csv')

def train_KNN(X, y, n_neighbors):
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)
    clf = KNeighborsClassifier(n_neighbors=n_neighbors)
    clf.fit(X_train, y_train)
    print(clf.score(X_test, y_test))
    #print(clf.feature_importances_)
    y_pred = clf.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    fakes = 0
    for y in y_pred:
        if y:
            fakes += 1
    return accuracy, f1, fakes

def hyper_KNN(path, name=None):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    y = df['is_fake']

    n_neighbors_mas = [1, 2, 3, 5, 10, 20]
    data = []
    columns = ['depth', 'accuracy', 'f1', 'fake_detected']

    for n_neighbors in n_neighbors_mas:
        accuracy, f1, fakes = train_KNN(X, y, n_neighbors)
        print(accuracy, f1, fakes)
        data.append([n_neighbors, accuracy, f1, fakes])

    df = pd.DataFrame(data, columns=columns)
    index = path.split('/')[-1].split('.')[0]
    # os.remove('tmp_my.csv')
    df.to_csv(f'C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\logs\\KNN_all.csv')


def train_SVM(X, y):
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)
    clf = svm.SVC(random_state=0)
    clf.fit(X_train, y_train)
    print(clf.score(X_test, y_test))
    #print(clf.feature_importances_)
    y_pred = clf.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    fakes = 0
    for y in y_pred:
        if y:
            fakes += 1
    return accuracy, f1, fakes

def hyper_SVM(path, name=None):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    X = pd.concat([X, df.loc[:, 'face_k':'area']], axis=1)
    y = df['is_fake']

    accuracy, f1, fakes = train_SVM(X, y)
    print(accuracy, f1, fakes)

def random_forest_EF_all():
    random_forest_EF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    random_forest_EF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\roop_orig.csv")
    random_forest_EF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\github_orig.csv")
    random_forest_EF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")

def random_forest_EF_FL_all():
    random_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\roop_orig.csv")
    os.remove('tmp_my.csv')
    random_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\github_orig.csv")
    os.remove('tmp_my.csv')
    random_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")
    os.remove('tmp_my.csv')

def random_forest_FL_all():
    random_forest_FL("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    os.remove('tmp_my.csv')
    random_forest_FL("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\roop_orig.csv")
    os.remove('tmp_my.csv')
    random_forest_FL("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\github_orig.csv")
    os.remove('tmp_my.csv')
    random_forest_FL("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")
    os.remove('tmp_my.csv')

def random_forest_LBP_all():
    random_forest_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    random_forest_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\roop_orig.csv")
    random_forest_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\github_orig.csv")
    random_forest_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")

def random_forest_EF_FL_LBP_all():
    random_forest_EF_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    os.remove('tmp_my.csv')
    random_forest_EF_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\roop_orig.csv")
    os.remove('tmp_my.csv')
    random_forest_EF_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\github_orig.csv")
    os.remove('tmp_my.csv')
    random_forest_EF_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")
    os.remove('tmp_my.csv')

def random_forest_FL_LBP_all():
    random_forest_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    os.remove('tmp_my.csv')
    random_forest_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\roop_orig.csv")
    os.remove('tmp_my.csv')
    random_forest_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\github_orig.csv")
    os.remove('tmp_my.csv')
    random_forest_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")
    os.remove('tmp_my.csv')

def find_best_FL_EF_LBP():
    best = -1
    acc = 0.5
    for i in range(5, 45):
        accuracy, f1, fakes = random_forest_EF_FL_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv",
                                                      i)
        if accuracy > acc:
            acc = accuracy
            best = i

    print(best, acc)

def find_best_FL_EF_LBP_TF_SF():
    best = -1
    acc = 0.5
    for i in range(5, 45):
        accuracy, f1, fakes = random_forest_EF_FL_LBP_TF_SF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv",
                                                      i)
        if accuracy > acc:
            acc = accuracy
            best = i

    print(best, acc)


def draw_points_new(path):
    random_forest_FL(path, True)
    os.remove('tmp_my.csv')
    mark_mesh(["C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\orig\\19613.jpg"], "test_out")

def random_forest_EF_extra(path, save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, 'face_k':'area']
    X = pd.concat([X, df.loc[:, 'eye_distance':'eye_slant_angle']], axis=1)
    X = pd.concat([X, df.loc[:, 'left_eye_aspect_ratio':'avg_eye_aspect_ratio']], axis=1)
    X = pd.concat([X, df.loc[:, 'eye_reflection_ratio_low':'eye_reflection_ratio_high']], axis=1)
    X = pd.concat([X, df.loc[:, 'hair_texture_std':'hair_contrast']], axis=1)
    X = X.drop('hair_color_std', axis=1)
    X = X.drop('hair_color_mean', axis=1)

    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, 20, save, name)
    print(accuracy, f1, fakes)

def random_forest_FL(path, save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477']

    X = get_x(X)
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, 20, save, name)
    print(accuracy, f1, fakes)


def random_forest_LBP(path, save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, 'hist_0':'hist_25']
    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, 20, save, name)
    print(accuracy, f1, fakes)

def random_forest_TF_SF_extra(path, depth = 20, is_save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, 'mean_intensity':'median_intensity']
    X = pd.concat([X, df.loc[:, 'contrast':'correlation']], axis=1)
    X = pd.concat([X, df.loc[:, 'skin_tone_std':'edge_density']], axis=1)
    X = pd.concat([X, df.loc[:, 'dct_std':'fft_high_freq_energy']], axis=1)
    X = pd.concat([X, df.loc[:, 'avg_face_depth':'depth_ratio']], axis=1)

    y = df['is_fake']
    accuracy, f1, fakes = train_forest(X, y, depth, is_save, name)
    print(accuracy, f1, fakes)
    return accuracy, f1, fakes

def catboost_EF_extra(path, save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape) #49
    X = df.loc[:, 'face_k':'area'] #12
    X = pd.concat([X, df.loc[:, 'eye_distance':'eye_slant_angle']], axis=1) #13
    X = pd.concat([X, df.loc[:, 'left_eye_aspect_ratio':'avg_eye_aspect_ratio']], axis=1) #12
    X = pd.concat([X, df.loc[:, 'eye_reflection_ratio_low':'eye_reflection_ratio_high']], axis=1) #3
    X = pd.concat([X, df.loc[:, 'hair_texture_std':'hair_contrast']], axis=1) #11
    X = X.drop('hair_color_std', axis=1) #1
    X = X.drop('hair_color_mean', axis=1) #1

    y = df['is_fake']
    accuracy, f1, fakes = train_catboost(X, y, 6, save, name)
    print(accuracy, f1, fakes)

def catboost_FL(path, save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, df.columns != 'is_fake']
    X = X.loc[:, '0':'477'] #478

    X = get_x(X)
    y = df['is_fake']
    accuracy, f1, fakes = train_catboost(X, y, 6, save, name)
    print(accuracy, f1, fakes)

def catboost_LBP(path, save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, 'hist_0':'hist_25'] #26
    y = df['is_fake']
    accuracy, f1, fakes = train_catboost(X, y, 6, save, name)
    print(accuracy, f1, fakes)

def catboost_TF_SF_extra(path, is_save=False, name="model.pth"):
    df = pd.read_csv(path)
    print(df.shape)
    X = df.loc[:, 'mean_intensity':'median_intensity'] #3
    X = pd.concat([X, df.loc[:, 'contrast':'correlation']], axis=1) #4
    X = pd.concat([X, df.loc[:, 'skin_tone_std':'edge_density']], axis=1) #12
    X = pd.concat([X, df.loc[:, 'dct_std':'fft_high_freq_energy']], axis=1) #15
    X = pd.concat([X, df.loc[:, 'avg_face_depth':'depth_ratio']], axis=1) #13

    y = df['is_fake']
    accuracy, f1, fakes = train_catboost(X, y, 6, is_save, name)
    print(accuracy, f1, fakes)
    return accuracy, f1, fakes

if __name__ == '__main__':
    # catboost_EF_extra("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train_aug.csv", True, "catboost_ef")
    # catboost_FL("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train_aug.csv", True, "catboost_fl")
    # catboost_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train_aug.csv", True, "catboost_lbp")
    # catboost_TF_SF_extra("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train_aug.csv",True, "catboost_tf_sf")

    path = "C:/Users/79101/Documents/Dataset/dataset_2000_3/df40_join_train_aug.csv"

    catboost_EF_extra(path, True, "catboost_ef")
    catboost_FL(path, True, "catboost_fl")
    catboost_LBP(path, True, "catboost_lbp")
    catboost_TF_SF_extra(path,True, "catboost_tf_sf")

    #random_forest_EF_extra("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train.csv", True, "random_forest_ef")
    #random_forest_FL("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train.csv", True, "random_forest_fl")
    #random_forest_LBP("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train.csv", True, "random_forest_lbp")
    #random_forest_TF_SF_extra("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\train.csv", 20,True, "random_forest_tf_sf")

    #os.remove('tmp_my.csv')
    #find_best_FL_EF_LBP_TF_SF()
    #random_forest_EF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv", is_save=False)
    #random_forest_all("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv", is_save=False)
    #random_forest_EF_FL_LBP_TF_SF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv", is_save=False)
    #random_forest_EF_FL_LBP_TF_SF("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv", 22)
    #draw_points_new("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")
    #random_forest_FL_LBP_all()
    #hyper_params_all("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv", "try_1")
    #random_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    #random_forest_FL_all()
    #hyper_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    #hyper_log_reg("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    #hyper_KNN("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    #hyper_SVM("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\all.csv")
    # hyper_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\roop_orig.csv")
    # hyper_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\github_orig.csv")
    # hyper_forest("C:\\Users\\79101\\Documents\\Dataset\\dataset_2000_3\\segmind_orig.csv")
