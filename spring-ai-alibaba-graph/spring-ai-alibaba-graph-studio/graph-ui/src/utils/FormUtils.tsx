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
