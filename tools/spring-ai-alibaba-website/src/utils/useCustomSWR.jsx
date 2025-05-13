import { useState } from "preact/hooks";
import useSWR from "swr";

const fetcher = (...args) => fetch(...args).then((res) => res.json());

const useCustomSWR = (api) => {
  const [shouldFetch, setShouldFetch] = useState(false);

  const { data, error } = useSWR(
    shouldFetch? api: null, 
    fetcher, {
      dedupingInterval: 3600000, // 1h
      revalidateOnFocus: false,
    }
  );

  const fetchData = () => {
    setShouldFetch(true);
  };

  const isLoading = !error && !data;

  return {
    swrData: data || {},
    error,
    isLoading,
    fetchData,
  };
}

export default useCustomSWR;