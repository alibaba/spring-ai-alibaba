## Reporting a Vulnerability

Please report any security issue or Spring AI Alibaba crash report to [ASRC](https://security.alibaba.com/)(Alibaba Security Response Center) where the issue will be triaged appropriately.

Thank you in advance for helping to keep Spring AI Alibaba secure.

## Local Development Credentials

These defaults are for local Docker / development only. Change them before any shared or production deployment.

### Spring AI Alibaba Admin (http://localhost/)

| Item | Value |
|------|-------|
| Username | `saa` |
| Password | `123456` |

The account is created by the Admin MySQL init script (`docker/middleware/init/mysql/agentscope-schema.sql`) as an admin user.

### Related local middleware defaults

| Service | Username | Password |
|---------|----------|----------|
| MySQL (`admin` database) | `admin` | `admin` |
| MySQL root | `root` | `root` |
| Nacos console (http://localhost:8848/nacos) | `nacos` | `nacos` |
