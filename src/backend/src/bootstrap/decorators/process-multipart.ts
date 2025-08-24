import {Readable} from "node:stream";

export async function processMultipart(
  parts: AsyncGenerator<any>,
  fileCallback: (file: {
    fieldname: string;
    filename: string;
    mimetype: string;
    encoding: string;
    stream: Readable;
  }) => Promise<void>,
  fieldCallback: (name: string, value: string) => void,
) {
  for await (const part of parts) {
    if (part.file) {
      await fileCallback({
        fieldname: part.fieldname,
        filename: part.filename,
        mimetype: part.mimetype,
        encoding: part.encoding,
        stream: part.file,
      });
    } else {
      fieldCallback(part.fieldname, part.value);
    }
  }
}
