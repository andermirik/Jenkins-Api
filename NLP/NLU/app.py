from flask import Flask, request, jsonify
import json
from rasa_nlu import Load_Rasa_NLU
from flask_cors import CORS

from transliterate import translit
from num2words import num2words
from urllib.parse import unquote

from fuzzywuzzy import process
from jenkins import JenkinsException

from babel.dates import format_datetime
from datetime import datetime, timedelta

nlu_model_path = "models/"
nlu_model = Load_Rasa_NLU(nlu_model_path)

app = Flask(__name__)
CORS(app)

import jenkins
jenkins_url = "http://localhost:8080"
username = "admin"
api_key = "11ba626ea393e7982b6a9c139e63bc14e0"

j_server = jenkins.Jenkins(jenkins_url, username=username, password=api_key)


def number_to_russian_words(number):
    if isinstance(number, float):
        integer_part, fractional_part = str(number).split('.')
        integer_part = int(integer_part)
        fractional_part = int(fractional_part)

        russian_integer = num2words(integer_part, lang='ru')
        russian_fractional = num2words(fractional_part, lang='ru')

        return f"{russian_integer} точка {russian_fractional}"
    else:
        return num2words(number, lang='ru')


def transliterate_to_russian(text):
    text = text.replace("'", "").replace('.', ' . ').replace(':', ' : ').replace(',', ' , ').replace('/', ' / ')
    transliterated = []
    words = text.split()

    for word in words:
        if '.' in word and all(part.isdigit() for part in word.split('.')):
            # this is a version, handle separately
            parts = word.split('.')
            word = " точка ".join(number_to_russian_words(int(part)) for part in parts)
        elif word.isdigit():
            word = number_to_russian_words(int(word))
        else:
            word = translit(word, 'ru')
        transliterated.append(word)

    return " ".join(transliterated)

def get_closest_job_name(input_job_name, job_names):
    # First, try to find a closest match among the original job names
    closest_match = process.extractOne(input_job_name, job_names, score_cutoff=70)
    
    # If no match was found, transliterate job names to Russian and try to find a match among them
    if closest_match is None:
        transliterated_job_names = [transliterate_to_russian(job_name) for job_name in job_names]
        print(transliterated_job_names)
        closest_match = process.extractOne(input_job_name, transliterated_job_names, score_cutoff=60)
        if closest_match is not None:
            # If a match was found among transliterated job names, return the original job name
            closest_job_name_index = transliterated_job_names.index(closest_match[0])
            return job_names[closest_job_name_index]
    
    # If a match was found among the original job names, return it
    elif closest_match:
        return closest_match[0]
    
    return None

def get_closest_existing_job_name(input_job_name):
    server_info = j_server.get_info()
    jobs = server_info.get("jobs")
    job_names = [job["name"] for job in jobs]

    closest_job_name = get_closest_job_name(input_job_name, job_names)
    if closest_job_name is None:
        return None

    return closest_job_name

def get_closest_parameter_name(input_parameter_name, parameter_names):
    closest_match = process.extractOne(input_parameter_name, parameter_names, score_cutoff=70)

    if closest_match is None:
        transliterated_parameter_names = [transliterate_to_russian(parameter_name) for parameter_name in parameter_names]
        closest_match = process.extractOne(input_parameter_name, transliterated_parameter_names, score_cutoff=60)
        if closest_match is not None:
            closest_parameter_name_index = transliterated_parameter_names.index(closest_match[0])
            return parameter_names[closest_parameter_name_index]
    
    elif closest_match:
        return closest_match[0]
    
    return None


# Определяем функции, соответствующие интентам
def get_server_info(**kwargs):
    server_info = j_server.get_info()
    user_info = j_server.get_whoami()
    version = j_server.get_version()

    mode = server_info.get("mode")
    num_executors = server_info.get("numExecutors")
    quieting_down = server_info.get("quietingDown")
    jobs = server_info.get("jobs")
    job_names = [job["name"] for job in jobs]

    full_name = user_info.get("fullName")
    permissions = user_info.get("permissions")

    response = f"Сервер Jenkins версии {version} работает в режиме {mode} с {num_executors} исполнителями. "
    
    if quieting_down:
        response += "Сервер сейчас приглушается. "
    else:
        response += "Сервер работает в обычном режиме. "
    
    response += f"Сейчас на сервере есть следующие задачи: {', '.join(job_names)}. "
    
    # if isinstance(permissions, (list, tuple, set)):
    #     response += f"Вы вошли как {full_name} с правами: {', '.join(permissions)}."
    # else:
    #     response += f"Вы вошли как {full_name}."

    return {"message": response, "for_tts": transliterate_to_russian(response)}



def get_all_jobs(**kwargs):
    server_info = j_server.get_info()
    jobs = server_info.get("jobs")
    job_names = [job["name"] for job in jobs]


    response = "Представляю вам список задач на сервере Jenkins: " + ', '.join(job_names)
    return {"message": response, "for_tts": transliterate_to_russian(response)}


def get_job_info(job_name, **kwargs):

    closest_job_name = get_closest_existing_job_name(job_name)
    if closest_job_name is None:
        response = f"Задача с именем {job_name} не существует."
        return {"message": response, "for_tts": transliterate_to_russian(response)}
    
    print(job_name, closest_job_name)
    try:
        job_info = j_server.get_job_info(closest_job_name)
    except jenkins.JenkinsException:
        response = f"Задача с именем {closest_job_name} не существует."
        return {"message": response, "for_tts": transliterate_to_russian(response)}


    # Извлекаем все доступные поля
    name = job_info.get('name', 'Неизвестно')
    description = job_info.get('description', 'Нет описания')
    if not description:
        description = "Нет описания"
    url = unquote(job_info.get('url', 'URL не указан'))
    buildable = 'да' if job_info.get('buildable', False) else 'нет'
    color = job_info.get('color', 'Неизвестно')
    first_build = job_info.get('firstBuild', {}).get('number', 'Нет информации') if job_info.get('firstBuild') else 'Нет информации'
    last_build = job_info.get('lastBuild', {}).get('number', 'Нет информации') if job_info.get('lastBuild') else 'Нет информации'
    last_completed_build = job_info.get('lastCompletedBuild', {}).get('number', 'Нет информации') if job_info.get('lastCompletedBuild') else 'Нет информации'
    last_failed_build = job_info.get('lastFailedBuild', {}).get('number', 'Нет информации') if job_info.get('lastFailedBuild') else 'Нет информации'
    last_stable_build = job_info.get('lastStableBuild', {}).get('number', 'Нет информации') if job_info.get('lastStableBuild') else 'Нет информации'
    last_successful_build = job_info.get('lastSuccessfulBuild', {}).get('number', 'Нет информации') if job_info.get('lastSuccessfulBuild') else 'Нет информации'
    last_unstable_build = job_info.get('lastUnstableBuild', {}).get('number', 'Нет информации') if job_info.get('lastUnstableBuild') else 'Нет информации'
    last_unsuccessful_build = job_info.get('lastUnsuccessfulBuild', {}).get('number', 'Нет информации') if job_info.get('lastUnsuccessfulBuild') else 'Нет информации'
    next_build_number = job_info.get('nextBuildNumber', 'Нет информации')
    concurrent_build = 'да' if job_info.get('concurrentBuild', False) else 'нет'
    
    response = f"Информация о задаче {name}:\n"
    response += f"Описание: {description}.\n"
    response += f"URL: {url}.\n"
    response += f"Возможность запуска сборки: {buildable}.\n"
    response += f"Статус: {color}.\n"
    response += f"Первая сборка: {first_build}.\n"
    response += f"Последняя сборка: {last_build}.\n"
    response += f"Последняя завершенная сборка: {last_completed_build}.\n"
    response += f"Последняя неудачная сборка: {last_failed_build}.\n"
    response += f"Последняя стабильная сборка: {last_stable_build}.\n"
    response += f"Последняя успешная сборка: {last_successful_build}.\n"
    response += f"Последняя нестабильная сборка: {last_unstable_build}.\n"
    response += f"Последняя неуспешная сборка: {last_unsuccessful_build}.\n"
    response += f"Номер следующей сборки: {next_build_number}.\n"
    response += f"Можно ли запустить параллельные сборки: {concurrent_build}.\n"

    return {"message": response, "for_tts": transliterate_to_russian(response)}


def unknown_action(**kwargs):
    return {"message": f"Простите, не понял ваш запрос"}

def trigger_job_build(job_name, **kwargs):
    try:
        closest_job_name = get_closest_existing_job_name(job_name)
        if closest_job_name is None:
            response = f"Задача с именем {job_name} не существует."
            return {"message": response, "for_tts": transliterate_to_russian(response)}
        
        # job = j_server.get_job_info(closest_job_name)

        # j_server.build_job(closest_job_name, default_parameters)
        response = f"Задача {closest_job_name} была запущена."
    except JenkinsException as e:
        response = f"Произошла ошибка при запуске задачи {job_name}: {str(e)}"
    return {"message": response, "for_tts": transliterate_to_russian(response)}

def stop_job_build(job_name, build_number, **kwargs):
    try:
        closest_job_name = get_closest_existing_job_name(job_name)
        if closest_job_name is None:
            response = f"Задача с именем {job_name} не существует."
            return {"message": response, "for_tts": transliterate_to_russian(response)}
        
        j_server.stop_build(closest_job_name, build_number)
        response = f"Сборка {build_number} задачи {closest_job_name} была остановлена."
    except JenkinsException as e:
        response = f"Произошла ошибка при остановке сборки {build_number} задачи {job_name}: {str(e)}"
    return {"message": response, "for_tts": transliterate_to_russian(response)}

def get_builds_list(job_name, **kwargs):
    job_name = get_closest_existing_job_name(job_name)
    if job_name is None:
        response = f"Задача с именем {job_name} не существует."
        return {"message": response, "for_tts": transliterate_to_russian(response)}
    else:
        try:
            job_info = j_server.get_job_info(job_name)
            builds = job_info.get('builds', [])
            builds_list = []
            for build in builds:
                build_info = j_server.get_build_info(job_name, build['number'])
                build_url = unquote(build_info['url'])
                build_result = build_info.get('result', 'В процессе')
                builds_list.append(f"Сборка номер {build['number']}, состояние: {build_result}, URL: {build_url}")
            response = "Список сборок: \n" + "\n".join(builds_list)
        except JenkinsException as e:
            response = f"Ошибка: {str(e)}"
        return {"message": response, "for_tts": transliterate_to_russian(response)}


def get_build_info(job_name, build_number, **kwargs):
    job_name = get_closest_existing_job_name(job_name)
    if job_name is None:
        response = f"Задача с именем {job_name} не существует."
        return {"message": response, "for_tts": transliterate_to_russian(response)}
    else:
        try:
            build_info = j_server.get_build_info(job_name, int(build_number))
            response = "Информация о сборке:\n"
            response += f"Номер сборки: {build_info.get('number')}\n"
            response += f"Результат сборки: {build_info.get('result')}\n"
            response += f"URL сборки: {build_info.get('url')}\n"
            
            timestamp = build_info.get('timestamp') / 1000  # Convert to seconds
            start_time = datetime.fromtimestamp(timestamp)  # Convert to datetime object
            start_time_str = format_datetime(start_time, "dd MMMM yyyy, HH:mm:ss", locale='ru_RU')
            response += f"Время начала сборки: {start_time_str}\n"

            duration = build_info.get('duration') / 1000  # Convert to seconds
            duration_td = timedelta(seconds=duration)  # Convert to timedelta object
            hours, remainder = divmod(duration_td.seconds, 3600)
            minutes, seconds = divmod(remainder, 60)
            duration_str = f"{hours} часов {minutes} минут {seconds} секунд"
            response += f"Продолжительность сборки: {duration_str}\n"

            response += f"Статус сборки: {'В процессе' if build_info.get('building') else 'Завершена'}\n"
        except JenkinsException as e:
            response = f"Ошибка: {str(e)}"
        return {"message": response, "for_tts": transliterate_to_russian(response)}

def get_job_build_console_output(job_name, build_number, **kwargs):
    job_name = get_closest_existing_job_name(job_name)
    if job_name is None:
        response = f"Задача с именем {job_name} не существует."
        return {"message": response, "for_tts": transliterate_to_russian(response)}
    else:
        try:
            console_output = j_server.get_build_console_output(job_name, int(build_number))
            response = f"Вывод консоли сборки {build_number} задачи {job_name}:\n"
            response += console_output
        except JenkinsException as e:
            response = f"Ошибка: {str(e)}"
        return {"message": response, "for_tts": transliterate_to_russian(response)}

def get_job_parameters(job_name, **kwargs):
    job_name = get_closest_existing_job_name(job_name)
    if job_name is None:
        response = f"Задача с именем {job_name} не существует."
        return {"message": response, "for_tts": transliterate_to_russian(response)}
    else:
        parameters = []
        try:
            job_info = j_server.get_job_info(job_name)
            if 'property' in job_info:
                for prop in job_info['property']:
                    if 'parameterDefinitions' in prop:
                        parameters = prop['parameterDefinitions']
                        break
                else:
                    response = "Для этой задачи не определены параметры."
            else:
                response = "Для этой задачи не определены параметры."
            
            if parameters:
                response = "Параметры задачи:\n"
                for param in parameters:
                    response += f"{param['name']}: {param['description']}.\n"
        except JenkinsException as e:
            response = f"Ошибка: {str(e)}"
        return {"message": response, "for_tts": transliterate_to_russian(response)}


def get_job_parameter_value(job_name, parameter_name, **kwargs):
    job_name = get_closest_existing_job_name(job_name)
    if job_name is None:
        response = f"Задача с именем {job_name} не существует."
        return {"message": response, "for_tts": transliterate_to_russian(response)}
    else:
        try:
            job_info = j_server.get_job_info(job_name)
            parameters = []
            if 'property' in job_info:
                for prop in job_info['property']:
                    if 'parameterDefinitions' in prop:
                        parameters = prop['parameterDefinitions']
                        break

            parameter_names = [param['name'] for param in parameters]
            closest_parameter_name = get_closest_parameter_name(parameter_name, parameter_names)

            if closest_parameter_name is not None:
                for param in parameters:
                    if param['name'] == closest_parameter_name:
                        if 'value' in param['defaultParameterValue']:
                            response = f"Значение параметра {param['name']}: {param['defaultParameterValue']['value']}"
                        else:
                            response = f"Параметр {param['name']} не имеет значения по умолчанию."
                        break
            else:
                response = "Параметр с таким именем не найден."
        except JenkinsException as e:
            response = f"Ошибка: {str(e)}"
        return {"message": response, "for_tts": transliterate_to_russian(response)}




intent_to_function = {
    "get_server_info": get_server_info,
    "get_all_jobs": get_all_jobs,
    "get_job_info": get_job_info,
    "unknown_action": unknown_action,
    "trigger_job_build": trigger_job_build,
    "stop_job_build": stop_job_build,
    "get_builds_list": get_builds_list,
    "get_build_info": get_build_info,
    "get_job_build_console_output": get_job_build_console_output,
    "get_job_parameters": get_job_parameters,
    "get_job_parameter_value": get_job_parameter_value
}

@app.route("/parse", methods=["POST"])
def parse():
    text = request.json.get("text")
    if not text:
        return jsonify({"error": "Text not provided"}), 400

    result = nlu_model.nlu_processing(text)
    print(result)
    result = json.loads(result)
    intent_name = result["intent"]["name"]
    confidence = result["intent"]["confidence"]

    if confidence <= 0.75:
        intent_name = "unknown_action"

    function = intent_to_function.get(intent_name)

    if function:
        entities = result.get("entities", [])
        slots = {entity["entity"]: entity["value"] for entity in entities}

        # Если нет распознанных слотов, передать наиболее вероятный параметр в виде слота
        if not slots and entities:
            most_probable_entity = max(entities, key=lambda x: x["confidence"])
            slots = {most_probable_entity["entity"]: most_probable_entity["value"]}

        response = function(**slots)
        return jsonify(response)
    else:
        return jsonify({"error": "Intent not supported"}), 400







if __name__ == "__main__":
    app.run(debug=True)
