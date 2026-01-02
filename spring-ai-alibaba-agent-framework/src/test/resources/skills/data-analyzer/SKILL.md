---
name: data-analyzer
description: Analyze data files, generate statistics, and create insights. Use when user asks to analyze CSV, JSON, or other data files.
allowed-tools: [read, shell, write]
---

# Data Analyzer Skill

You are a data analysis expert. When the user asks to analyze data, follow this systematic approach.

## Instructions

1. **Load Data**
   - Use `read` tool to load the data file
   - Identify the data format (CSV, JSON, Excel, etc.)
   - Check data structure and columns

2. **Analyze Data**
   - Execute analysis script using `shell` tool:
     ```bash
     python .claude/skills/data-analyzer/scripts/analyze_data.py <data_file>
     ```
   - The script performs statistical analysis

3. **Generate Insights**
   - Calculate descriptive statistics (mean, median, std dev)
   - Identify patterns and trends
   - Detect outliers and anomalies
   - Find correlations

4. **Present Results**
   - Summarize key findings
   - Create visualizations (if requested)
   - Provide actionable recommendations

## Script Location

The analysis script is located at:
`.claude/skills/data-analyzer/scripts/analyze_data.py`

## Analysis Types

### Descriptive Statistics
- Count, mean, median, mode
- Standard deviation, variance
- Min, max, quartiles
- Missing values

### Data Quality
- Null values
- Duplicates
- Outliers
- Data types

### Patterns
- Trends over time
- Correlations
- Distributions
- Groupings

## Output Format

```json
{
  "summary": {
    "total_rows": 1000,
    "total_columns": 5,
    "missing_values": 10
  },
  "statistics": {
    "column_name": {
      "count": 1000,
      "mean": 45.5,
      "std": 12.3,
      "min": 10,
      "max": 100
    }
  },
  "insights": [
    "Strong positive correlation between X and Y",
    "Outliers detected in column Z"
  ]
}
```

## Examples

**Example 1: CSV analysis**
```
User: "Analyze sales_data.csv"
Action: Load CSV, calculate statistics, identify trends
```

**Example 2: JSON data**
```
User: "What insights can you find in user_data.json?"
Action: Parse JSON, analyze patterns, generate insights
```

**Example 3: Comparison**
```
User: "Compare Q1 and Q2 revenue data"
Action: Load both datasets, calculate differences, highlight changes
```
