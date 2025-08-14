import request from './request';
export { getPreviewUrl } from './download';

// Type definition for upload request parameters
type TUploadRequest = {
  file: File | any; // File to be uploaded
  category: string; // File category
  onProgress?: (event: { percent: number }) => void; // Progress callback
};

// Type definition for upload response
type TUploadResponse = Promise<{
  extension: string; // File extension
  name: string; // Original filename
  path: string; // Storage path
  size: number; // File size in bytes
}>;

// Enum for different upload methods
export enum UPLOAD_METHOD {
  OSS = 'oss', // Aliyun OSS upload
  FILE = 'file', // Direct file upload
}

/**
 * Main upload function that routes to appropriate method based on config
 * @param data Upload request parameters
 * @returns Promise with upload response
 */
export default function (data: TUploadRequest): TUploadResponse {
  const upload_method = window.g_config.config.upload_method;

  if (upload_method === UPLOAD_METHOD.OSS) return ossUpload(data);
  if (upload_method === UPLOAD_METHOD.FILE) return fileUpload(data);
  return fileUpload(data); // Default to file upload
}

/**
 * Direct file upload implementation
 * @param data Upload request parameters
 * @returns Promise with upload response
 */
export function fileUpload(data: TUploadRequest): TUploadResponse {
  const formData = new FormData();
  formData.append('category', data.category);
  formData.append('files', data.file);

  return request({
    method: 'POST',
    url: '/console/v1/files/upload',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress(progressEvent) {
      data.onProgress?.({
        percent: Math.min(
          1,
          progressEvent.loaded / (progressEvent?.total || 1),
        ),
      });
    },
  }).then((res) => {
    return res.data.data[0]; // Return first file from response
  });
}

/**
 * OSS (Aliyun Object Storage Service) upload implementation
 * @param data Upload request parameters
 * @returns Promise with upload response
 */
export async function ossUpload(data: TUploadRequest): TUploadResponse {
  // First get OSS upload configuration from server
  const ossUploadConfigRes = await request({
    url: '/console/v1/files/upload-policies',
    method: 'POST',
    data: {
      files: [
        {
          name: data.file.name,
        },
      ],
      category: data.category,
    },
  });

  const ossUploadConfig = ossUploadConfigRes.data.data[0];

  // Prepare form data with OSS required fields
  const formData = new FormData();
  formData.append('OSSAccessKeyId', ossUploadConfig.access_id);
  formData.append('success_action_status', '200');
  formData.append('policy', ossUploadConfig.policy);
  formData.append('signature', ossUploadConfig.signature);
  formData.append('key', ossUploadConfig.path);
  formData.append('file', data.file);

  // Upload file directly to OSS
  return request({
    method: 'POST',
    url: ossUploadConfig.host,
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress(progressEvent) {
      data.onProgress?.({
        percent: Math.min(
          1,
          progressEvent.loaded / (progressEvent?.total || 1),
        ),
      });
    },
  }).then(() => {
    // Return file metadata after successful upload
    return {
      extension: getFileExtension(data.file.name),
      name: data.file.name,
      path: ossUploadConfig.path,
      size: data.file.size,
    };
  });
}

/**
 * Helper function to extract file extension from filename
 * @param filename Original filename
 * @returns File extension without dot
 */
function getFileExtension(filename: string): string {
  const lastDotIndex = filename.lastIndexOf('.');
  return (lastDotIndex > 0 ? filename.slice(lastDotIndex) : '').replace(
    /\./,
    '',
  );
}

//// Example usage
// import { request } from "@/request"
// import { Button, Upload } from 'antd';
// import upload from "@/request/upload";

// export default function () {
//   return <div>
//     <Upload
//       customRequest={(options) => {
//         upload({
//           file: options.file,
//           category: 'document',
//           onProgress({ percent }) {
//             options.onProgress?.({
//               percent
//             });
//           }
//         }).then(res => {
//           options.onSuccess?.(res)
//         }).catch(err => {
//           options.onError?.(err)
//         });
//       }}
//     >
//       <Button>upload</Button>
//     </Upload>
//   </div>
// }
