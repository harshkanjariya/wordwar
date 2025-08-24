export const onSendHook = async (_: any, reply: any, payload: any) => {
  try {
    if (reply.statusCode >= 200 && reply.statusCode < 300) {
      return JSON.stringify({
        status: reply.statusCode,
        data: JSON.parse(payload),
      });
    }
    return payload;
  } catch (error) {
    return payload;
  }
};
