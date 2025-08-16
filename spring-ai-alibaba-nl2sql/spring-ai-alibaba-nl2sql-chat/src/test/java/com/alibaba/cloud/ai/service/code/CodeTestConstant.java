/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service.code;

/**
 * CodeExecutor测试的代码常量
 *
 * @author vlsmb
 * @since 2025/7/29
 */
public final class CodeTestConstant {

	private CodeTestConstant() {

	}

	static final String NORMAL_CODE = """
			def func(x: int):
			    if x <= 0:
			        return 1;
			    else:
			        return x * func(x-1)
			if __name__ == "__main__":
			    print(func(10))
			""";

	static final String CODE_WITH_DEPENDENCY = """
			import numpy as np

			matrix = np.array([[1, 2], [3, 4]])
			inverse_matrix = np.linalg.inv(matrix)

			print(matrix)
			print(inverse_matrix)
			""";

	static final String TIMEOUT_CODE = """
			while True:
				continue
			""";

	static final String ERROR_CODE = """
			void main() {}
			""";

	static final String NEED_INPUT = """
			print(input())
			""";

	static final String STUDENT_SCORE_ANALYSIS = """
			import json
			import sys
			from collections import defaultdict

			def main():
			    # 从标准输入读取JSON数据
			    data = json.load(sys.stdin)

			    # 1. 计算每个学生的总分和平均分
			    subjects = set()
			    for student in data:
			        scores = student["scores"]
			        student["total_score"] = sum(scores.values())
			        student["average_score"] = round(student["total_score"] / len(scores), 2)
			        subjects.update(scores.keys())

			    # 2. 按总分排序并计算排名
			    data.sort(key=lambda x: (-x["total_score"], x["student_id"]))
			    rank = 1
			    prev_score = None
			    for i, student in enumerate(data):
			        if prev_score is not None and student["total_score"] < prev_score:
			            rank = i + 1
			        student["rank"] = rank
			        prev_score = student["total_score"]

			    # 3. 按班级分组统计
			    class_stats = defaultdict(lambda: {
			        "subject_avg": {subj: [] for subj in subjects},
			        "subject_max": {subj: float('-inf') for subj in subjects},
			        "subject_min": {subj: float('inf') for subj in subjects},
			        "class_avg_total": [],
			        "class_max_total": float('-inf'),
			        "class_min_total": float('inf')
			    })

			    for student in data:
			        cls = student["class"]
			        total = student["total_score"]
			        stats = class_stats[cls]

			        # 更新班级总分统计
			        stats["class_avg_total"].append(total)
			        stats["class_max_total"] = max(stats["class_max_total"], total)
			        stats["class_min_total"] = min(stats["class_min_total"], total)

			        # 更新各科目统计
			        for subj, score in student["scores"].items():
			            stats["subject_avg"][subj].append(score)
			            stats["subject_max"][subj] = max(stats["subject_max"][subj], score)
			            stats["subject_min"][subj] = min(stats["subject_min"][subj], score)

			    # 4. 计算班级平均分并构建结果
			    class_summary = {}
			    for cls, stats in class_stats.items():
			        class_summary[cls] = {
			            "subjects": {
			                subj: {
			                    "average": round(sum(scores)/len(scores), 2),
			                    "max": stats["subject_max"][subj],
			                    "min": stats["subject_min"][subj]
			                } for subj, scores in stats["subject_avg"].items()
			            },
			            "total_score": {
			                "average": round(sum(stats["class_avg_total"])/len(stats["class_avg_total"]), 2),
			                "max": stats["class_max_total"],
			                "min": stats["class_min_total"]
			            }
			        }

			    # 5. 构建最终结果
			    result = {
			        "students": [{
			            "student_id": s["student_id"],
			            "name": s["name"],
			            "class": s["class"],
			            "total_score": s["total_score"],
			            "average_score": s["average_score"],
			            "rank": s["rank"]
			        } for s in data],
			        "class_summary": class_summary
			    }

			    print(json.dumps(result, indent=2, ensure_ascii=False))

			if __name__ == "__main__":
			    main()
			""";

	static final String STUDENT_SCORE_ANALYSIS_INPUT = """
			[
			  {
			    "student_id": "S001",
			    "name": "User1",
			    "class": "ClassA",
			    "scores": {
			      "math": 92,
			      "english": 88,
			      "science": 95
			    }
			  },
			  {
			    "student_id": "S002",
			    "name": "User2",
			    "class": "ClassB",
			    "scores": {
			      "math": 85,
			      "english": 92,
			      "science": 89
			    }
			  },
			  {
			    "student_id": "S003",
			    "name": "User3",
			    "class": "ClassA",
			    "scores": {
			      "math": 78,
			      "english": 85,
			      "science": 92
			    }
			  },
			  {
			    "student_id": "S004",
			    "name": "User4",
			    "class": "ClassB",
			    "scores": {
			      "math": 95,
			      "english": 90,
			      "science": 87
			    }
			  }
			]
			""";

	static final String ECOMMERCE_SALES_PANDAS_CODE = """
			import pandas as pd
			import numpy as np
			import json
			import sys
			from datetime import datetime
			import warnings
			warnings.filterwarnings('ignore')

			# 从标准输入读取JSON数据
			input_data = sys.stdin.read()
			input_json = json.loads(input_data)

			# 解析输入JSON数据
			df = pd.DataFrame(input_json['data'])

			# 数据预处理
			df['date'] = pd.to_datetime(df['date'])
			df['total_price'] = df['quantity'] * df['unit_price'] * (1 - df['discount'])
			df['month'] = df['date'].dt.month
			df['weekday'] = df['date'].dt.day_name()

			# 构建输出JSON对象
			output_result = {}

			# 基本统计信息
			output_result['basic_stats'] = {
			    'total_records': len(df),
			    'date_range': {
			        'start': df['date'].min().strftime('%Y-%m-%d'),
			        'end': df['date'].max().strftime('%Y-%m-%d')
			    },
			    'total_sales': round(df['total_price'].sum(), 2),
			    'total_orders': len(df),
			    'average_order_value': round(df['total_price'].mean(), 2)
			}

			# 产品销售分析
			product_analysis = df.groupby('product').agg({
			    'total_price': ['sum', 'count', 'mean'],
			    'quantity': 'sum'
			}).round(2)
			product_analysis.columns = ['total_sales', 'order_count', 'avg_order_value', 'total_quantity']

			output_result['product_analysis'] = {}
			for product in product_analysis.index:
			    output_result['product_analysis'][product] = {
			        'total_sales': float(product_analysis.loc[product, 'total_sales']),
			        'order_count': int(product_analysis.loc[product, 'order_count']),
			        'avg_order_value': float(product_analysis.loc[product, 'avg_order_value']),
			        'total_quantity': int(product_analysis.loc[product, 'total_quantity'])
			    }

			# 类别销售分析
			category_analysis = df.groupby('category')['total_price'].agg(['sum', 'count', 'mean']).round(2)
			output_result['category_analysis'] = {}
			for category in category_analysis.index:
			    output_result['category_analysis'][category] = {
			        'total_sales': float(category_analysis.loc[category, 'sum']),
			        'order_count': int(category_analysis.loc[category, 'count']),
			        'avg_order_value': float(category_analysis.loc[category, 'mean'])
			    }

			# 时间趋势分析
			monthly_analysis = df.groupby('month')['total_price'].agg(['sum', 'count']).round(2)
			output_result['monthly_analysis'] = {}
			for month in monthly_analysis.index:
			    output_result['monthly_analysis'][int(month)] = {
			        'total_sales': float(monthly_analysis.loc[month, 'sum']),
			        'order_count': int(monthly_analysis.loc[month, 'count'])
			    }

			# 客户分析
			customer_analysis = df.groupby('customer_id').agg({
			    'total_price': ['sum', 'count'],
			    'date': 'nunique'
			}).round(2)
			customer_analysis.columns = ['total_spent', 'order_count', 'active_days']
			customer_analysis['avg_order_value'] = customer_analysis['total_spent'] / customer_analysis['order_count']

			output_result['customer_analysis'] = {}
			for customer_id in customer_analysis.index:
			    output_result['customer_analysis'][int(customer_id)] = {
			        'total_spent': float(customer_analysis.loc[customer_id, 'total_spent']),
			        'order_count': int(customer_analysis.loc[customer_id, 'order_count']),
			        'active_days': int(customer_analysis.loc[customer_id, 'active_days']),
			        'avg_order_value': float(customer_analysis.loc[customer_id, 'avg_order_value'])
			    }

			# 相关性分析
			correlation_data = df[['quantity', 'unit_price', 'discount', 'total_price']]
			correlation_matrix = correlation_data.corr().round(4)
			output_result['correlation_analysis'] = {}
			for col1 in correlation_matrix.columns:
			    output_result['correlation_analysis'][col1] = {}
			    for col2 in correlation_matrix.columns:
			        output_result['correlation_analysis'][col1][col2] = float(correlation_matrix.loc[col1, col2])

			# 排名信息
			output_result['rankings'] = {
			    'top_products_by_sales': df.groupby('product')['total_price'].sum().sort_values(ascending=False).head(3).to_dict(),
			    'top_categories_by_sales': df.groupby('category')['total_price'].sum().sort_values(ascending=False).head(3).to_dict(),
			    'top_customers_by_spending': df.groupby('customer_id')['total_price'].sum().sort_values(ascending=False).head(3).to_dict()
			}

			# 格式化排名数据，确保键为字符串
			for key in output_result['rankings']:
			    output_result['rankings'][key] = {str(k): float(v) for k, v in output_result['rankings'][key].items()}

			# 将numpy数据类型转换为Python原生类型
			def convert_numpy_types(obj):
			    if isinstance(obj, np.integer):
			        return int(obj)
			    elif isinstance(obj, np.floating):
			        return float(obj)
			    elif isinstance(obj, np.ndarray):
			        return obj.tolist()
			    elif isinstance(obj, dict):
			        return {key: convert_numpy_types(value) for key, value in obj.items()}
			    elif isinstance(obj, list):
			        return [convert_numpy_types(item) for item in obj]
			    else:
			        return obj

			output_result = convert_numpy_types(output_result)

			# 输出JSON对象到控制台
			print(json.dumps(output_result, ensure_ascii=False, indent=2))
			""";

	static final String ECOMMERCE_SALES_PANDAS_INPUT = """
			{
			  "data": [
			    {"date": "2023-01-01", "product": "手机", "category": "电子产品", "quantity": 2, "unit_price": 3999.00, "discount": 0.1, "customer_id": 1001},
			    {"date": "2023-01-01", "product": "耳机", "category": "配件", "quantity": 1, "unit_price": 299.00, "discount": 0.05, "customer_id": 1002},
			    {"date": "2023-01-02", "product": "笔记本电脑", "category": "电子产品", "quantity": 1, "unit_price": 8999.00, "discount": 0.15, "customer_id": 1003},
			    {"date": "2023-01-02", "product": "智能手表", "category": "智能设备", "quantity": 3, "unit_price": 1299.00, "discount": 0.0, "customer_id": 1004},
			    {"date": "2023-01-03", "product": "平板电脑", "category": "电子产品", "quantity": 1, "unit_price": 2999.00, "discount": 0.2, "customer_id": 1001},
			    {"date": "2023-01-03", "product": "手机", "category": "电子产品", "quantity": 1, "unit_price": 3999.00, "discount": 0.1, "customer_id": 1005},
			    {"date": "2023-01-04", "product": "耳机", "category": "配件", "quantity": 2, "unit_price": 299.00, "discount": 0.0, "customer_id": 1006},
			    {"date": "2023-01-04", "product": "智能手表", "category": "智能设备", "quantity": 1, "unit_price": 1299.00, "discount": 0.05, "customer_id": 1007},
			    {"date": "2023-01-05", "product": "笔记本电脑", "category": "电子产品", "quantity": 2, "unit_price": 8999.00, "discount": 0.12, "customer_id": 1008},
			    {"date": "2023-01-05", "product": "平板电脑", "category": "电子产品", "quantity": 1, "unit_price": 2999.00, "discount": 0.18, "customer_id": 1002},
			    {"date": "2023-01-06", "product": "手机", "category": "电子产品", "quantity": 3, "unit_price": 3999.00, "discount": 0.08, "customer_id": 1009},
			    {"date": "2023-01-06", "product": "耳机", "category": "配件", "quantity": 1, "unit_price": 299.00, "discount": 0.15, "customer_id": 1010},
			    {"date": "2023-01-07", "product": "智能手表", "category": "智能设备", "quantity": 2, "unit_price": 1299.00, "discount": 0.0, "customer_id": 1003},
			    {"date": "2023-01-07", "product": "笔记本电脑", "category": "电子产品", "quantity": 1, "unit_price": 8999.00, "discount": 0.2, "customer_id": 1011},
			    {"date": "2023-01-08", "product": "手机", "category": "电子产品", "quantity": 1, "unit_price": 3999.00, "discount": 0.05, "customer_id": 1004}
			  ]
			}
			""";

}
