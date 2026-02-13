import datetime

def get_current_week_range():
    today = datetime.date.today()
    start_of_week = today - datetime.timedelta(days=today.weekday())
    end_of_week = start_of_week + datetime.timedelta(days=4)
    start_str = start_of_week.strftime('%Y-%m-%d')
    end_str = end_of_week.strftime('%Y-%m-%d')
    return f"{start_str} ~ {end_str}"

if __name__ == "__main__":
    result = get_current_week_range()
    # Print a single, structured range string for easy parsing by tools.
    print(result)

