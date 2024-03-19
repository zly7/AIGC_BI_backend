### 这一套完全没有看langChian的文档，所以思路不对
import os
from flask import Flask, request, jsonify
from langchain_openai import OpenAI  # 新的导入方式
# 确保你已经安装了langchain库

app = Flask(__name__)

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

if __name__ == '__main__':
    app.run(debug=True, port=5000)
