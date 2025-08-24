const fuzzyEdits = 0;

export const atlasIndex = {
  user: {
    name: (query: string) => ({
      index: 'user-name-index',
      autocomplete: {
        query, path: 'name',
        fuzzy: fuzzyEdits >= 1 ? { maxEdits: fuzzyEdits } : undefined,
      }
    }),
  },
  product: {
    name: (query: string) => ({
      index: 'product-name-index',
      compound: {
        should: [
          {
            autocomplete: {
              query: query,
              path: 'name',
              fuzzy: fuzzyEdits >= 1 ? { maxEdits: fuzzyEdits } : undefined,
            }
          },
          {
            autocomplete: {
              query: query,
              path: 'description',
              fuzzy: fuzzyEdits >= 1 ? { maxEdits: fuzzyEdits } : undefined,
            }
          }
        ]
      }
    }),
  }
};
