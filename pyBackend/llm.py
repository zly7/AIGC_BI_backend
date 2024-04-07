### 这一套完全没有看langChian的文档，所以思路不对
import os
from flask import Flask, request, jsonify, request, render_template
from langchain import OpenAI  # 新的导入方式
from langchain.agents.agent_types import AgentType
from langchain.chat_models import ChatOpenAI
from langchain_experimental.agents.agent_toolkits import create_pandas_dataframe_agent
import pandas
from io import BytesIO
import httpx

# 确保你已经安装了langchain库

app = Flask(__name__, template_folder='template')

api_key = os.getenv('OPENAI_API_KEY')
if api_key is not None:
    # 环境变量存在，可以使用api_key了
    print(f"OpenAI API key found in environment variables:{api_key}")
    llm = OpenAI(api_key=api_key)
else:
    # 环境变量不存在，处理这种情况（例如，显示错误信息）
    print("ERROR: OpenAI API key not found in environment variables.")


@app.route('/process_prompt', methods=['POST'])
def process_prompt():
    data = request.json
    prompt = data.get('prompt')
    # 新增：从请求中获取模型ID
    model_id = data.get('model_id')

    if not prompt or not model_id:
        return jsonify({"error": "Missing 'prompt' or 'model_id'"}), 400

    # 使用提供的模型ID调用generate方法
    response = llm.generate([prompt], model=model_id, max_tokens=5000)  # 根据需要调整max_tokens和使用model_id

    return jsonify({"response": response.text})


# 做langchain处理文件实验
@app.route('/upload', methods=['POST'])
def upload_file():
    # Check if a file was uploaded
    if 'file' not in request.files:
        return 'No file part'

    file = request.files['file']

    # If user does not select file, browser also
    # submit an empty part without filename
    if file.filename == '':
        return 'No selected file'
    openai = OpenAI(temperature=0)
    print("OpenAI model used", openai.model_name)
    # Save the file to a folder (you might want to change this path)
    df = pandas.read_csv(file)
    agent = create_pandas_dataframe_agent(
        ChatOpenAI(temperature=0, model="gpt-3.5-turbo-0613"),
        df,
        verbose=True,
        agent_type=AgentType.OPENAI_FUNCTIONS,
        handle_parsing_errors=True
    )
    results = agent("Analyze this data")
    # Optionally, you can redirect the user to another page after successful upload
    return results

from typing_extensions import override
from openai import AssistantEventHandler
class EventHandler(AssistantEventHandler):
    @override
    def on_text_created(self, text) -> None:
        print(f"\nassistant > ", end="", flush=True)

    @override
    def on_text_delta(self, delta, snapshot):
        print(delta.value, end="", flush=True)

    def on_tool_call_created(self, tool_call):
        print(f"\nassistant > {tool_call.type}\n", flush=True)

    def on_tool_call_delta(self, delta, snapshot):
        if delta.type == 'code_interpreter':
            if delta.code_interpreter.input:
                print(delta.code_interpreter.input, end="", flush=True)
            if delta.code_interpreter.outputs:
                print(f"\n\noutput >", flush=True)
                for output in delta.code_interpreter.outputs:
                    if output.type == "logs":
                        print(f"\n{output.logs}", flush=True)

@app.route('/conversation', methods=['POST'])
def conversation():
    from openai import OpenAI
    client = OpenAI(
        api_key=api_key
    )

    if 'file' not in request.files:
        return 'No file part'

    # Get the file from the request
    file = request.files['file']

    # Read file contents as bytes
    file_contents = file.read()

    # Create a BytesIO object to wrap the file contents
    file_buffer = BytesIO(file_contents)

    # Pass the file data to the OpenAI client
    file_openai = client.files.create(
        file=file_buffer,
        purpose='assistants'
    )

    assistant = client.beta.assistants.create(
        name="Data visualizer",
        description="A test assistant to visualize",
        model="gpt-4",
        tools=[{"type": "code_interpreter"}],
        file_ids=[file_openai.id]
    )

    thread = client.beta.threads.create()

    run = client.beta.threads.runs.create_and_poll(
        thread_id=thread.id,
        assistant_id=assistant.id,
        instructions="You are a data analyst,I will give you the raw data, "
                    "you need to help me summarize it according to the requirements. Please format it as required, "
                    "dividing it into two parts, the first part is the front-end Echarts V5 option configuration object "
                    "JavaScript code, and the second part is the analysis of the data result, visualize the data reasonably, "
                    "without generating any superfluous content. Start both parts with ##### .",

    )

    if run.status == 'completed':
        messages = client.beta.threads.messages.list(
            thread_id=thread.id
        )
        print(messages)
    else:
        print(run.status)
    # with client.beta.threads.runs.stream(
    #         thread_id=thread.id,
    #         assistant_id=assistant.id,
    #         instructions="Do the instruction that i have given in the given assistant",
    #         event_handler=EventHandler(),
    # ) as stream:
    #     stream.until_done()

@app.route('/')
def upload_form():
    return render_template('upload.html')


# @app.route('/process_excel', methods=['POST'])
# def process_prompt():
#     data = request.json
#     prompt = data.get('prompt')
#     # 新增：从请求中获取模型ID
#     model_id = data.get('model_id')
#
#     if not prompt or not model_id:
#         return jsonify({"error": "Missing 'prompt' or 'model_id'"}), 400
#
#     # 使用提供的模型ID调用generate方法
#     response = llm.generate([prompt], model=model_id, max_tokens=5000)  # 根据需要调整max_tokens和使用model_id
#
#     return jsonify({"response": response.text})

if __name__ == '__main__':
    app.run(debug=True, port=5000,use_reloader=False)
