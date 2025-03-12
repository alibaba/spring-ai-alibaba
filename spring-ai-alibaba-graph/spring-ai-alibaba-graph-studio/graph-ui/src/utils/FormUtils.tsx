/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  FormButtonGroup,
  FormDrawer,
  FormItem,
  FormLayout,
  Input,
  Submit,
} from '@formily/antd-v5';
import { createSchemaField, ISchema } from '@formily/react';

//! Create custom components uniformly here.
const SchemaField = createSchemaField({
  components: {
    FormItem,
    Input,
  },
});

interface IOptions<T> {
  data?: Partial<T>;
  onConfirm?: (values: T) => void;
}

export const openPanel = <T,>(schema: ISchema, options?: IOptions<T>) => {
  FormDrawer('Drawer', () => {
    return (
      <FormLayout labelCol={6} wrapperCol={10}>
        <SchemaField schema={schema} />
        <FormDrawer.Extra>
          <FormButtonGroup align="right">
            <Submit
              onSubmit={() => {
                return Promise.resolve();
              }}
            >
              Submit
            </Submit>
            {/* <Reset>Reset</Reset> */}
          </FormButtonGroup>
        </FormDrawer.Extra>
      </FormLayout>
    );
  })
    // .forOpen((payload, next) => {
    //   next(payload)
    // })
    .open({
      initialValues: options?.data,
    })
    .then((value: T) => {
      console.log('confirm', value);
      options?.onConfirm?.(value);
    });
};
