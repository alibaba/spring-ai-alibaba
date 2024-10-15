# Spring AI 用户使用示例

## 生成 SQL 应用

根据接口传入的参数生成查询 SQL 语句。

使用示例如下：

**当输入的参数为 email 时**

> url: http://localhost:8081/ai/sql?text="email"

```shell
$ curl http://localhost:8081/ai/sql?text=%22email%22 | python -m json.tool
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   355    0   355    0     0    514      0 --:--:-- --:--:-- --:--:--   514
{
    "sqlQuery": "SELECT email FROM TBL_USER;",
    "results": [
        {
            "EMAIL": "user1@example.com"
        },
        {
            "EMAIL": "user2@example.com"
        },
        {
            "EMAIL": "user3@example.com"
        },
        {
            "EMAIL": "user4@example.com"
        },
        {
            "EMAIL": "user5@example.com"
        },
        {
            "EMAIL": "user6@example.com"
        },
        {
            "EMAIL": "user7@example.com"
        },
        {
            "EMAIL": "user8@example.com"
        },
        {
            "EMAIL": "user9@example.com"
        },
        {
            "EMAIL": "user10@example.com"
        }
    ]
}
```

**不输入任何参数时**

```shell
$ curl http://localhost:8081/ai/sql | python -m json.tool
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   844    0   844    0     0   1115      0 --:--:-- --:--:-- --:--:--  1116
{
    "sqlQuery": "SELECT * FROM TBL_USER;",
    "results": [
        {
            "ID": 1,
            "USERNAME": "user1",
            "EMAIL": "user1@example.com",
            "PASSWORD": "password1"
        },
        {
            "ID": 2,
            "USERNAME": "user2",
            "EMAIL": "user2@example.com",
            "PASSWORD": "password2"
        },
        {
            "ID": 3,
            "USERNAME": "user3",
            "EMAIL": "user3@example.com",
            "PASSWORD": "password3"
        },
        {
            "ID": 4,
            "USERNAME": "user4",
            "EMAIL": "user4@example.com",
            "PASSWORD": "password4"
        },
        {
            "ID": 5,
            "USERNAME": "user5",
            "EMAIL": "user5@example.com",
            "PASSWORD": "password5"
        },
        {
            "ID": 6,
            "USERNAME": "user6",
            "EMAIL": "user6@example.com",
            "PASSWORD": "password6"
        },
        {
            "ID": 7,
            "USERNAME": "user7",
            "EMAIL": "user7@example.com",
            "PASSWORD": "password7"
        },
        {
            "ID": 8,
            "USERNAME": "user8",
            "EMAIL": "user8@example.com",
            "PASSWORD": "password8"
        },
        {
            "ID": 9,
            "USERNAME": "user9",
            "EMAIL": "user9@example.com",
            "PASSWORD": "password9"
        },
        {
            "ID": 10,
            "USERNAME": "user10",
            "EMAIL": "user10@example.com",
            "PASSWORD": "password10"
        }
    ]
}
```

### 词性分型应用

将输入的文本进行词性分析，并输出分析结果。

> url: http://localhost:8082/ai/text/structured-output?text="好人"

```shell
$ curl http://localhost:8082/ai/text/structured-output?text=%22%E5%A5%BD%E4%BA%BA%22

"POSITIVE"
```
