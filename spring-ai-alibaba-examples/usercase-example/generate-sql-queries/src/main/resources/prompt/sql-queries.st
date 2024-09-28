Given the DDL in the DDL section, write an SQL query that answers the asked question in the QUESTION section.
Only produce select queries. Do not append any text or markup in the start or end of response.
Remove the markups such as ``` , sql , \n as well.
If the question would result in an insert, update, or delete, or if the query would alter the DDL in any way, say that the operation isn't supported.
If the question can't be answered, say that the DDL doesn't support answering that question.

QUESTION
{question}

DDL
{ddl}
