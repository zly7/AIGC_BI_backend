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
    # chartTypePath: str # english name of the chart type

class ReadTemplateForChartType(BaseModel):
    chartTypeChinese: str
    chartTypeEnglish: str
    template: str
    def read_template(self, prefix: str = "./template"):
        # 构建文件路径
        file_path = os.path.join(prefix, f"{self.chartType}.txt")
        
        # 检查文件是否存在
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"No file found at {file_path}")
        
        # 读取文件内容
        with open(file_path, 'r', encoding='utf-8') as file:
            self.template = file.read()

MapTemplateForChartType = {}
files = os.listdir("./template")
txt_files = [file for file in files if file.endswith('.txt')]
for file in txt_files:
    with open(f"./template/{file}", 'r', encoding='utf-8') as f:
# 读取第一行作为键
        first_line_key = f.readline().strip().replace("/", "")
        # 读取文件剩余部分作为模板内容
        template_content = f.read()
        # 使用第一行作为键，存储模板内容
        MapTemplateForChartType[first_line_key] = ReadTemplateForChartType(chartTypeChinese=first_line_key, 
                                                    chartTypeEnglish=file,template=template_content)
  
# vectorstore = DocArrayInMemorySearch.from_texts(
#     ["You are a data analyst"],
#     embedding=OpenAIEmbeddings(),
# )
# retriever = vectorstore.as_retriever()

template = ("You are a data analyst,I will give you the raw data, "
            "you need to help me summarize it according to the requirements. Please format it as required, "
            "dividing it into two parts, the first part is the front-end Echarts V5 option configuration object "
            "JavaScript code, and the second part is the analysis of the data result "
            "without generating any superfluous content. Start both parts of Echarts V5 JS code or analysis result with ##### . You answer should start with '''My analysis are placed below includes two parts which are eacharts js code and analysis'''\n"
            "User's analysis requirement is:\n{goal}\n"
            "The final chart type to be generated is:\n{chart_type}\n"
            "I will give you one example below of this chart type to help you understand the requirements."
            "The original data in CSV format is:\n{csv_string}, after generating the chart, you will only do analysis based on the chart you've created."
            "Please generate the full content of the chart, do not use ellipses which is ... to abbreviate. Note that the generated echarts option will be process by JS eval() function, so make sure the format is correct."
            "My analysis are placed below includes two parts which are eacharts js code and analysis #####\n"
            "The option configuration object js code for frontend Echarts V5 can effectively visualize data.\n" +
            "Clear data analysis conclusions, the more detailed the better, and do not generate extraneous comments."
            "I will give one real example of the same chart type below, you must follow the format of the example. But the eachats format can be different because of different requirements. Note that the generated echarts option should start directly from {{, without any other formatting."
            "My analysis are placed below includes two parts which are eacharts js code and analysis. #####\n"
            """{template_for_this_chart_type}"""
            "#####\n Place your analysis here."
            "\n\n\nFinally, please start your creation. Follow the format and the example I provided to generate a chart with a similar structure, but use the CSV data I provided."
            "To clarify, I would like to confirm a few requirements regarding the format of the response:"
            "1.There should be only two '#####' in the answer, and the initial expression at the beginning of the answer should be 'My analysis are placed below includes two parts which are Echarts JS code and analysis.'"
            "2.The Echarts JS code should be sandwiched between the two '#####', and there's no need to use '```' to denote markdown code."
            "3.The final analysis should follow after the second '#####'."
            "4.The example I gave you is of this type of chart; please try to refer to this example as much as possible. Frontend will use eval() to process your result."
            "5.Please ensure that the generated chart is cognitively coherent, especially since the x and y axes are often easily confused and reversed."
            "6.Please ensure that the penultimate line of code is 'const option = {{...}};' and the last line is 'option;'. This allows the outer 'eval()' to read the returned 'option'.")
prompt = ChatPromptTemplate.from_template(template)
# ChatOpenAI(temperature=0.7, model="deepseek-chat",base_url="https://api.deepseek.com",api_key="sk-473258b4e169450db3e503745fe70c99")
if not use_proxy_clash:
    model_3_dot_5 = ChatOpenAI(model_name="gpt-3.5-turbo", openai_api_key=api_key,openai_api_base="https://api.openai-proxy.com/v1/")
    model_4_dot_0 = ChatOpenAI(model_name="gpt-4", openai_api_key=api_key,openai_api_base="https://api.openai-proxy.com/v1")
else:
    model_3_dot_5 = ChatOpenAI(model_name="gpt-3.5-turbo", openai_api_key=api_key)
    model_4_dot_0 = ChatOpenAI(model_name="gpt-4", openai_api_key=api_key)
ZHIPUAI_API_KEY = os.getenv("ZHIPUAI_API_KEY")
model_glm_4 = ChatOpenAI(
    temperature=0.7,
    model="glm-4",
    openai_api_key=ZHIPUAI_API_KEY,
    openai_api_base="https://open.bigmodel.cn/api/paas/v4"
)
model_glm_4_air = ChatOpenAI(
    temperature=0.7,
    model="glm-4-air",
    openai_api_key=ZHIPUAI_API_KEY,
    openai_api_base="https://open.bigmodel.cn/api/paas/v4"
)
model_glm_3_turbo = ChatOpenAI(
    temperature=0.7,
    model="glm-3-turbo",
    openai_api_key=ZHIPUAI_API_KEY,
    openai_api_base="https://open.bigmodel.cn/api/paas/v4"
)
output_parser = StrOutputParser()

# setup_and_retrieval = RunnableParallel(
#     {"context": retriever, "question": RunnablePassthrough()}
# )
chain_3 =  prompt | model_3_dot_5 | output_parser
chain_4 =  prompt | model_4_dot_0 | output_parser
glm_chain_4 = prompt | model_glm_4 | output_parser
glm_chain_4_air = prompt | model_glm_4_air | output_parser
glm_chain_3_turbo = prompt | model_glm_3_turbo | output_parser
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
    elif data_package.modelName == "glm-4":
        chain = glm_chain_4
    elif data_package.modelName == "glm-4-air":
        chain = glm_chain_4_air
    elif data_package.modelName == "glm-3-turbo":
        chain = glm_chain_3_turbo
    elif "3.5" in data_package.modelName:
        chain = chain_3
        print("Using gpt-3.5-turbo")
    print(data_package)
    result = chain.invoke({"goal": data_package.goal, "chart_type": data_package.chartType, "csv_string": data_package.csvString, "template_for_this_chart_type": MapTemplateForChartType[data_package.chartType].template})
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
        description=template,
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
    app.run(debug=False, port=5000)