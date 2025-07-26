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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.config.ContainerConfiguration;
import com.alibaba.cloud.ai.tool.PythonExecutorTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { ContainerConfiguration.class })
@DisplayName("Run Python Code in Docker Test Without Network")
@ActiveProfiles("docker")
public class DockerContainerPoolExecutorTest {

	private static final Logger log = LoggerFactory.getLogger(DockerContainerPoolExecutorTest.class);

	@Autowired
	private PythonExecutorTool pythonExecutorTool;

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

	static final String NETWORK_CHECK = """
			import socket
			import urllib.request

			ans1 = "C" + "o" + "nnected"
			ans2 = "F" + "a" + "iled"
			if __name__ == "__main__":
			    try:
			        socket.gethostbyname("www.aliyun.com")
			        print("DNS " + ans1)
			    except:
			        print("DNS " + ans2)
			    try:
			        with urllib.request.urlopen("http://www.aliyun.com", timeout=3) as response:
			            print("HTTP " + ans1)
			    except Exception as e:
			        print(f"HTTP {ans2}: {str(e)}")
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

	private void testNormalCode() {
		log.info("Run Normal Code");
		String response = pythonExecutorTool.executePythonCode(NORMAL_CODE, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("3628800");
		log.info("Run Normal Code Finished");
	}

	private void testCodeWithDependency() {
		log.info("Run Code with Third-parties Installed");
		String response = pythonExecutorTool.executePythonCode(CODE_WITH_DEPENDENCY, "numpy==2.2.6", "DataFrame Data");
		System.out.println(response);
		assertThat(response).doesNotContain("ModuleNotFoundError");
		log.info("Run Code with Third-parties Installed Finished");
	}

	private void testTimeoutCode() {
		log.info("Run Code with Endless Loop");
		String response = pythonExecutorTool.executePythonCode(TIMEOUT_CODE, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("Killed");
		log.info("Run Code with Endless Loop Finished");
	}

	private void testErrorCode() {
		log.info("Run Code with Syntax Error");
		String response = pythonExecutorTool.executePythonCode(ERROR_CODE, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("SyntaxError");
		log.info("Run Code with Syntax Error Finished");
	}

	private void testNetworkCheck() {
		log.info("Run Network Check");
		String response = pythonExecutorTool.executePythonCode(NETWORK_CHECK, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("Connected").doesNotContain("Failed");
		log.info("Run Network Check Finished");
	}

	private void testNeedInput() {
		log.info("Check Need Input");
		String response = pythonExecutorTool.executePythonCode(NEED_INPUT, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("DataFrame Data");
		log.info("Run Need Input Finished");
	}

	private void testStudentScoreAnalysis() {
		log.info("Run Student Score Analysis");
		String response = pythonExecutorTool.executePythonCode(STUDENT_SCORE_ANALYSIS, null,
				STUDENT_SCORE_ANALYSIS_INPUT);
		System.out.println(response);
		assert StringUtils.hasText(response);
		log.info("Run Student Score Analysis Finished");
	}

	@Test
	@DisplayName("Concurrency Testing")
	public void testConcurrency() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		CountDownLatch countDownLatch = new CountDownLatch(7);
		executorService.submit(() -> {
			try {
				this.testNormalCode();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				countDownLatch.countDown();
			}
		});
		executorService.submit(() -> {
			try {
				this.testCodeWithDependency();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				countDownLatch.countDown();
			}
		});
		executorService.submit(() -> {
			try {
				this.testTimeoutCode();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				countDownLatch.countDown();
			}
		});
		executorService.submit(() -> {
			try {
				this.testErrorCode();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				countDownLatch.countDown();
			}
		});
		executorService.submit(() -> {
			try {
				this.testNetworkCheck();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				countDownLatch.countDown();
			}
		});
		executorService.submit(() -> {
			try {
				this.testNeedInput();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				countDownLatch.countDown();
			}
		});
		executorService.submit(() -> {
			try {
				this.testStudentScoreAnalysis();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				countDownLatch.countDown();
			}
		});
		assert countDownLatch.await(600L, TimeUnit.SECONDS);
	}

}
