-- Add a table containing additional information about users that helps us to reduce metric pollution
-- (for now, whether a user is a member of the RmS team or not)

CREATE TABLE user_meta
(
    user_id     TEXT NOT NULL,
    email       TEXT NOT NULL DEFAULT '<unknown>',
    affiliation TEXT NOT NULL DEFAULT 'customer',
    UNIQUE (user_id)
);
