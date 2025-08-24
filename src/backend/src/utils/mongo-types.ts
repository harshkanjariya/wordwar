import {Filter, SortDirection} from "mongodb";
import {FullDocument} from "../shared/types/api";

export type SearchStage = {
  $search: {
    index: string;
    autocomplete?: {
      query: string;
      path: string;
      fuzzy?: {
        maxEdits: number;
      };
    };
    compound?: {
      should: Array<{
        autocomplete: {
          query: string;
          path: string;
          fuzzy?: {
            maxEdits: number;
          };
        };
      }>;
    };
  };
};


export type MatchStage<T> = {
  $match: Filter<FullDocument<T>>;
};

export type SkipStage = {
  $skip: number;
};

export type SortStage<T> = {
  $sort: SortType<T>;
};

export type SortType<T> = Partial<Record<keyof FullDocument<T>, SortDirection>>;

export type LimitStage = {
  $limit: number;
};

export type ProjectStage<T> = {
  $project: Projection<T>;
}

export type Projection<T> = {
  [K in keyof (FullDocument<T> | any)]?: 1 | 0;
};

export type LookupStage = {
  $lookup: {
    from: string;
    localField: string;
    foreignField: string;
    as: string;
  }
}

export type UnwindStage = {
  $unwind: {
    path: string;
    preserveNullAndEmptyArrays?: boolean;
  }
}

export type PipelineStage<T> =
  SearchStage
  | MatchStage<T>
  | ProjectStage<T>
  | SkipStage
  | LimitStage
  | SortStage<T>
  | LookupStage
  | UnwindStage;

export type QueryOptions<T> = {
  filter?: Filter<FullDocument<T>>;
  project?: Projection<T>;
  sort?: SortType<T>;
  skip?: number;
  limit?: number;
};
