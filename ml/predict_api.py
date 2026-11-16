from flask import Flask, request, jsonify
from flask_cors import CORS
import os
from werkzeug.utils import secure_filename

from predict import ImageClassifier

app = Flask(__name__)

# Список разрешенных адресов для CORS
ALLOWED_ORIGINS = [
    "26.236.50.212:8080",
    "26.72.3.168:5000",
    "localhost:5000",
]

CORS(app, resources={
    r"/predict": {
        "origins": ALLOWED_ORIGINS,
        "methods": ["POST"],
        "allow_headers": ["Content-Type"]
    }
})

# Конфигурация для загрузки файлов
UPLOAD_FOLDER = 'uploads'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# Создаем папку для загрузок, если ее нет
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Инициализация классификатора (один раз при старте сервера)
classifier = ImageClassifier()


def allowed_file(filename):
    return '.' in filename and \
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/predict', methods=['POST'])
def predict():
    try:
        # Проверяем Origin (если нужно)
        origin = request.headers.get('Host')
        print(origin)
        print(request.headers)
        if not origin or origin not in ALLOWED_ORIGINS:
            return jsonify({'error': 'Origin not allowed'}), 403

        # Проверка наличия файла
        if 'file' not in request.files:
            return jsonify({'error': 'No file provided'}), 400

        file = request.files['file']

        if file.filename == '':
            return jsonify({'error': 'Empty filename'}), 400

        if not allowed_file(file.filename):
            return jsonify({'error': 'Invalid file type'}), 400

        # Сохраняем временный файл
        filename = secure_filename(file.filename)
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)

        try:
            # Получаем предсказание
            result = classifier.predict_image(filepath)

            # Проверяем структуру результата
            if not isinstance(result, dict) or 'probability' not in result or 'class' not in result:
                raise ValueError("Invalid result format from classifier")

            # Формируем ответ
            response = {
                'image_name': filename,
                'fake_probability': round(float(result['probability']), 4),
                'class': int(result['class']),
                'class_description': 'Fake' if int(result['class']) == 1 else 'Real'
            }

            return jsonify(response)

        except ValueError as e:
            if "No face found" in str(e):
                return jsonify({'error': 'No face detected in the image'}), 422
            return jsonify({'error': str(e)}), 400
        except KeyError as e:
            return jsonify({'error': f"Missing expected data in result: {str(e)}"}), 500
        except Exception as e:
            app.logger.error(f"Classification error: {str(e)}", exc_info=True)
            return jsonify({'error': 'No face detected in the image'}), 422

        finally:
            # Удаляем временный файл
            if os.path.exists(filepath):
                os.remove(filepath)

    except Exception as e:
        app.logger.error(f"Server error: {str(e)}", exc_info=True)
        return jsonify({'error': 'Internal server error', 'type': type(e).__name__}), 500


if __name__ == '__main__':
    app.run(host='localhost', port=5000, debug=True)