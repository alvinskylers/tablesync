CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
ADD CONSTRAINT no_overlapping_reservations EXCLUDE USING gist (
    table_id WITH =,
    tsrange(reservation_start, reservation_end) WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED'));