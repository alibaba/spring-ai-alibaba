import $i18n from '@/i18n';
import upload, { getPreviewUrl } from '@/request/upload';
import uniqueId from '@/utils/uniqueId';
import { Button, IconFont, Input } from '@spark-ai/design';
import { Flex, Popover, Typography, Upload } from 'antd';
import classNames from 'classnames';
import { memo, useEffect, useState } from 'react';
import styles from './index.module.less';

const isImageFile = (file: File): boolean => {
  return /^image\/(jpeg|png|gif|bmp|webp)$/i.test(file.type);
};

const getFileSize = (size: number): string => {
  if (size < 1024) {
    return `${size}B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(2)}KB`;
  }

  return `${(size / 1024 / 1024).toFixed(2)}MB`;
};

const sourceLabelMap = {
  localFile: $i18n.get({
    id: 'main.components.VariableBaseInput.FileInput.local',
    dm: '本地',
  }),
  remoteUrl: $i18n.get({
    id: 'main.components.VariableBaseInput.FileInput.link',
    dm: '链接',
  }),
};

interface IFile {
  name: string;
  type: 'image' | 'document' | 'audio' | 'video' | 'custom';
  size?: number;
  source: 'localFile' | 'remoteUrl';
  url: string;
  id: string;
  percent?: number;
  previewUrl?: string;
  mime_type?: string;
}

export interface IFileRemoteUrlInputFormProps {
  onSubmit: (val: IFile) => void;
  onCancel: () => void;
  disabled?: boolean;
}

export const RemoteLinkFileItem = memo(
  ({ file, onDelete }: { file: IFile; onDelete: () => void }) => {
    return (
      <Flex className={styles['file-item']} gap={8} align="center">
        <div className={styles['file-logo']}>
          <IconFont
            className={styles['file-logo-icon']}
            type="spark-link-line"
          />
        </div>
        <Flex className="flex-1 w-1" vertical>
          <Typography.Text
            ellipsis={{ tooltip: file.name }}
            className={styles['file-name']}
          >
            {file.name}
          </Typography.Text>
          <div className={styles['file-size']}>
            {sourceLabelMap[file.source]}
          </div>
        </Flex>
        <IconFont onClick={onDelete} isCursorPointer type="spark-delete-line" />
      </Flex>
    );
  },
);

export const FileRemoteUrlInputForm = memo(
  (props: IFileRemoteUrlInputFormProps) => {
    const [url, setUrl] = useState('');

    const onSubmit = () => {
      if (!url.trim()) {
        return;
      }

      props.onSubmit({
        id: uniqueId(4),
        name: url.split('/').pop() || url,
        type: 'custom',
        source: 'remoteUrl',
        url: url,
        percent: 1,
      });
    };

    return (
      <div className={styles['remote-url-input-form']}>
        <div className={styles['remote-url-input-form-title']}>
          {$i18n.get({
            id: 'main.components.VariableBaseInput.FileInput.link',
            dm: '链接',
          })}
        </div>
        <Input
          placeholder={$i18n.get({
            id: 'main.components.VariableBaseInput.FileInput.enterLink',
            dm: '请输入链接',
          })}
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />

        <Flex className={styles['remote-url-input-form-actions']} gap={8}>
          <Button disabled={props.disabled} onClick={onSubmit} type="primary">
            {$i18n.get({
              id: 'main.components.VariableBaseInput.FileInput.submit',
              dm: '提交',
            })}
          </Button>
          <Button>
            {$i18n.get({
              id: 'main.components.VariableBaseInput.FileInput.cancel',
              dm: '取消',
            })}
          </Button>
        </Flex>
      </div>
    );
  },
);

export const FileInput = memo(
  ({
    value,
    onChange,
    isSingle,
  }: {
    value?: string;
    onChange: (value: string) => void;
    isSingle?: boolean;
  }) => {
    const [files, setFiles] = useState<IFile[]>([]);
    const [open, setOpen] = useState(false);

    useEffect(() => {
      if (!value) setFiles([]);
    }, [value]);

    useEffect(() => {
      const newFiles: Omit<IFile, 'percent' | 'id'>[] = [];

      files.forEach((item) => {
        if (item.percent !== 1) return;
        newFiles.push({
          name: item.name,
          type: item.type,
          size: item.size,
          source: item.source,
          url: item.url,
          mime_type: item.mime_type,
        });
      });

      if (isSingle) {
        onChange(JSON.stringify(newFiles[0]));
      } else {
        onChange(JSON.stringify(newFiles));
      }
    }, [files]);

    const addRemoteFile = (file: IFile) => {
      setFiles((prev) => [...prev, file]);
    };

    const handleDelete = (file: IFile) => {
      setFiles((prev) => prev.filter((f) => f.id !== file.id));
    };

    return (
      <div
        className={classNames(styles['file-input'], {
          [styles.hasFiles]: !!files.length,
        })}
      >
        <Flex gap={8}>
          <Upload
            fileList={[]}
            className={styles['upload-btn-wrap']}
            multiple={!isSingle}
            maxCount={isSingle ? 1 : undefined}
            disabled={isSingle && files.length >= 1}
            customRequest={(options) => {
              const file = options.file as File;
              const newFile: IFile = {
                id: uniqueId(4),
                percent: 0,
                name: file.name as string,
                type: 'custom',
                source: 'localFile',
                size: file.size,
                url: '',
                mime_type: file.type,
              };
              setFiles((prev) => [...prev, newFile]);
              upload({
                file: options.file,
                category: 'temp',
                onProgress: (v) => {
                  setFiles((prev) =>
                    prev.map((f) =>
                      f.id === newFile.id ? { ...f, percent: v.percent } : f,
                    ),
                  );
                },
              }).then(async (res) => {
                const extra = isImageFile(file)
                  ? {
                      previewUrl: await getPreviewUrl(res.path),
                    }
                  : {};
                setFiles((prev) =>
                  prev.map((f) =>
                    f.id === newFile.id
                      ? { ...f, percent: 1, url: res.path, ...extra }
                      : f,
                  ),
                );
              });
            }}
          >
            <Button
              className="w-full"
              variant="filled"
              disabled={isSingle && files.length >= 1}
              icon={<IconFont type="spark-upload-line" />}
            >
              {$i18n.get({
                id: 'main.components.VariableBaseInput.FileInput.local',
                dm: '本地',
              })}
            </Button>
          </Upload>
          <Popover
            open={open && !(isSingle && files.length >= 1)}
            onOpenChange={setOpen}
            destroyTooltipOnHide
            content={
              <FileRemoteUrlInputForm
                disabled={isSingle && files.length >= 1}
                onSubmit={(val) => {
                  addRemoteFile(val);
                  setOpen(false);
                }}
                onCancel={() => {
                  setOpen(false);
                }}
              />
            }
            trigger={['click']}
          >
            <Button
              disabled={isSingle && files.length >= 1}
              className="flex-1"
              variant="filled"
              icon={<IconFont type="spark-link-line" />}
            >
              {$i18n.get({
                id: 'main.components.VariableBaseInput.FileInput.link',
                dm: '链接',
              })}
            </Button>
          </Popover>
        </Flex>
        <Flex className={styles['file-list']} vertical gap={12}>
          {files.map((file) => {
            if (file.source === 'remoteUrl')
              return (
                <RemoteLinkFileItem
                  onDelete={() => handleDelete(file)}
                  file={file}
                  key={file.id}
                />
              );

            return (
              <Flex
                className={styles['file-item']}
                gap={8}
                align="center"
                key={file.id}
              >
                {file.previewUrl ? (
                  <img
                    alt={file.name}
                    className={styles['file-preview']}
                    src={file.previewUrl}
                  />
                ) : (
                  <div className={styles['file-logo']}>
                    {file.percent !== 1 && file.percent !== void 0 ? (
                      <IconFont
                        className={styles['file-loading-icon']}
                        type="spark-loading-line"
                      />
                    ) : (
                      <IconFont type="spark-imageFiles-line" />
                    )}
                  </div>
                )}
                <Flex className="flex-1 w-1" vertical>
                  <Typography.Text
                    ellipsis={{ tooltip: file.name }}
                    className={styles['file-name']}
                  >
                    {file.name}
                  </Typography.Text>
                  {file.percent !== 1 && file.percent !== void 0 ? (
                    <div className={styles['file-progress']}>
                      {$i18n.get({
                        id: 'main.components.VariableBaseInput.FileInput.uploadProgress',
                        dm: '上传进度&nbsp;',
                      })}
                      {parseInt(`${file.percent * 100}`)}%
                    </div>
                  ) : (
                    <div className={styles['file-size']}>
                      {getFileSize(file.size || 0)}/
                      {sourceLabelMap[file.source]}
                    </div>
                  )}
                </Flex>
                <IconFont
                  onClick={() => handleDelete(file)}
                  isCursorPointer
                  type="spark-delete-line"
                />
              </Flex>
            );
          })}
        </Flex>
      </div>
    );
  },
);
