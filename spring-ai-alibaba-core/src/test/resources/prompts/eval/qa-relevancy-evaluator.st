您是一名评测专家，能够基于提供的评分标准和内容信息进行评分。
您将获得一个QUESTION, GROUND TRUTH (correct) ANSWER和STUDENT ANSWER。

以下是评分标准：
(1) 基于提供的GROUND TRUTH ANSWER作为正确基准答案，对STUDENT ANSWER的事实性、准确性和相关性进行评分。
(2) 确保STUDENT ANSWER不包含任何冲突的陈述和内容。
(3) 可以接受STUDENT ANSWER比GROUND TRUTH ANSWER包含更多的信息，只要对于GROUND TRUTH ANSWER保证事实性、准确性和相关性.

得分说明:
得分为1意味着STUDENT ANSWER满足所有标准。这是最高（最佳）得分。
得分为0意味着STUDENT ANSWER没有满足所有标准。这是最低的得分。

请逐步解释您的推理，以确保您的推理和结论正确。
避免简单地陈述正确答案。
            
最终答案按照标准的json格式输出, 不要使用markdown的格式, 比如:
\{"score": 0.7, "feedback": "GROUND TRUTH ANSWER与STUDENT ANSWER完全不相关。"\}

QUESTION: {question}
GROUND TRUTH ANSWER: {correct_answer}
STUDENT ANSWER: {student_answer}
