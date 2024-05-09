# Requires:
# pip install langchain docarray tiktoken

# from langchain_community.vectorstores import DocArrayInMemorySearch
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableParallel, RunnablePassthrough
# from langchain_community.chat_models import ChatOpenAI
from langchain_openai.chat_models import ChatOpenAI
from langchain.agents.openai_assistant import OpenAIAssistantRunnable
import os
from pydantic import BaseModel
from flask import Flask, request, jsonify,render_template
import openai
use_proxy_clash = True
if use_proxy_clash:
    openai.proxy = {
        "http": "127.0.0.1:7890",
        "https": "127.0.0.1:7890"
    }
api_key = os.getenv('OPENAI_API_KEY')
if api_key is not None:
    # 环境变量存在，可以使用api_key了
    print(f"OpenAI API key found in environment variables:{api_key}")
else:
    print("ERROR: OpenAI API key not found in environment variables.")

class GiveLangChainManagerDataPackage(BaseModel):
    chartType: str
    goal: str
    csvString: str
    modelName: str
    chartId: int

# vectorstore = DocArrayInMemorySearch.from_texts(
#     ["You are a data analyst"],
#     embedding=OpenAIEmbeddings(),
# )
# retriever = vectorstore.as_retriever()

template = ("You are a data analyst,I will give you the raw data, "
            "you need to help me summarize it according to the requirements. Please format it as required, "
            "dividing it into two parts, the first part is the front-end Echarts V5 option configuration object "
            "JavaScript code, and the second part is the analysis of the data result "
            "without generating any superfluous content. Start both parts of Echarts V5 JSON code or analysis result with ##### . You answer should start with '''My analysis are placed below includes two parts which are eacharts js code and analysis'''\n"
            "User's analysis requirement is:\n{goal}\n"
            "The final chart type to be generated is:\n{chart_type}\n"
            "The original data in CSV format is:\n{csv_string}, after generating the chart, you will only do analysis based on the chart you've created."
            "Please generate the full content of the chart, do not use ellipses which is ... to abbreviate. Note that the generated echarts option will be parsed by JSON parser to get the final option object in JS, so make sure the format is correct."
            "I will give the example below, you must follow the format of the example. But the eachats format can be different because of different requirements. Note that the generated echarts option should start directly from {{, without any other formatting."
            "My analysis are placed below includes two parts which are eacharts js code and analysis #####\n"
            """{{
                "title": {{
                    "text": "网站增长情况"
                }},
                "tooltip": {{
                    "trigger": "axis"
                }},
                "grid": {{
                    "left": "3%",
                    "right": "4%",
                    "bottom": "3%",
                    "containLabel": true
                }},
                "toolbox": {{
                    "feature": {{
                    "saveAsImage": {{}}
                    }}
                }},
                "xAxis": {{
                    "type": "category",
                    "boundaryGap": false,
                    "data": ["1月1日", "1月2日", "1月3日", "1月4日", "1月5日"]
                }},
                "yAxis": {{
                    "type": "value"
                }},
                "series": [
                    {{
                    "name": "增长数量",
                    "type": "bar",
                    "data": [10, 20, 30, 40, 50],
                    "itemStyle": {{
                        "color": "#3398DB"
                    }}
                    }}
                ]
                }}\n"""
            "#####\n"
            "The analysis of the data result")
prompt = ChatPromptTemplate.from_template(template)
if not use_proxy_clash:
    model_3_dot_5 = ChatOpenAI(model_name="gpt-3.5-turbo", openai_api_key=api_key,openai_api_base="https://api.openai-proxy.com/v1/")
    model_4_dot_0 = ChatOpenAI(model_name="gpt-4", openai_api_key=api_key,openai_api_base="https://api.openai-proxy.com/v1")
else:
    model_3_dot_5 = ChatOpenAI(model_name="gpt-3.5-turbo", openai_api_key=api_key)
    model_4_dot_0 = ChatOpenAI(model_name="gpt-4", openai_api_key=api_key)
output_parser = StrOutputParser()

# setup_and_retrieval = RunnableParallel(
#     {"context": retriever, "question": RunnablePassthrough()}
# )
chain_3 =  prompt | model_3_dot_5 | output_parser
chain_4 =  prompt | model_4_dot_0 | output_parser

app = Flask(__name__)
from openai import OpenAI
client = OpenAI()
@app.route('/all_llm_process', methods=['POST'])
def process_prompt():
    try:
        # 使用pydantic的模型解析和校验请求中的JSON数据
        data_package = GiveLangChainManagerDataPackage(**request.json)
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 400
    if data_package.modelName == "gpt-3.5-turbo":
        chain = chain_3
    elif data_package.modelName == "gpt-4":
        chain = chain_4
    elif "3.5" in data_package.modelName:
        chain = chain_3
        print("Using gpt-3.5-turbo")
    print(data_package)
    result = chain.invoke({"goal": data_package.goal, "chart_type": data_package.chartType, "csv_string": data_package.csvString})
    print(result)
    return result


@app.route('/')
def upload_form():
    return render_template('upload.html')



@app.route('/conversation', methods=['POST'])
def conversation():
    if 'file' not in request.files:
        return 'No file part'

    # Get the file from the request
    file = request.files['file']

    assistant = client.beta.assistants.create(
        name="Data visualizer",
        description="You are a data analyst,I will give you the raw data, "
            "you need to help me summarize it according to the requirements. Please format it as required, "
            "dividing it into two parts, the first part is the front-end Echarts V5 option configuration object "
            "JavaScript code, and the second part is the analysis of the data result "
            "without generating any superfluous content. Start both parts of Echarts V5 JSON code or analysis result with ##### . You answer should start with '''My analysis are placed below includes two parts which are eacharts js code and analysis'''\n"
            "User's analysis requirement is:\n{goal}\n"
            "The final chart type to be generated is:\n{chart_type}\n"
            "The original data in CSV format is:\n{csv_string}, after generating the chart, you will only do analysis based on the chart you've created."
            "Please generate the full content of the chart, do not use ellipses which is ... to abbreviate. Note that the generated echarts option will be parsed by JSON parser to get the final option object in JS, so make sure the format is correct."
            "I will give the example below, you must follow the format of the example. But the eachats format can be different because of different requirements. Note that the generated echarts option should start directly from {{, without any other formatting."
            "My analysis are placed below includes two parts which are eacharts js code and analysis #####\n"
            """{{
                "title": {{
                    "text": "网站增长情况"
                }},
                "tooltip": {{
                    "trigger": "axis"
                }},
                "grid": {{
                    "left": "3%",
                    "right": "4%",
                    "bottom": "3%",
                    "containLabel": true
                }},
                "toolbox": {{
                    "feature": {{
                    "saveAsImage": {{}}
                    }}
                }},
                "xAxis": {{
                    "type": "category",
                    "boundaryGap": false,
                    "data": ["1月1日", "1月2日", "1月3日", "1月4日", "1月5日"]
                }},
                "yAxis": {{
                    "type": "value"
                }},
                "series": [
                    {{
                    "name": "增长数量",
                    "type": "bar",
                    "data": [10, 20, 30, 40, 50],
                    "itemStyle": {{
                        "color": "#3398DB"
                    }}
                    }}
                ]
                }}\n"""
            "#####\n"
            "The analysis of the data result",
        model="gpt-4",
        tools=[{"type": "code_interpreter"}],
        file_ids=[file.id]
    )

    thread = client.beta.threads.create()

    run = client.beta.threads.runs.create_and_poll(
        thread_id=thread.id,
        assistant_id=assistant.id,
        instructions="Do the instruction that i have given in the given assistant"
    )

    if run.status == 'completed':
        messages = client.beta.threads.messages.list(
            thread_id=thread.id
        )
        print(messages)
    else:
        print(run.status)

if __name__ == '__main__':
    app.run(debug=True, port=5000)