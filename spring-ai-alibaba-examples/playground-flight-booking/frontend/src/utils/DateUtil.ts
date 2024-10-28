import dayjs from "dayjs";

// Format time
export const formattedDate = (date: string) => {
  return date && dayjs(date).format("YYYY-MM-DD HH:mm:ss");
};
