#!/bin/bash

if [[ -z "$STAGE" ]]; then
  STAGE="review"
fi

if [[ -z "$PROJECT_IDENTIFIER" ]]; then
  PROJECT_IDENTIFIER="7845582f-e262-77b0-e2c6-183d49cca5be"
fi

if [[ -z "$ACCESS_TOKEN" ]]; then
  ACCESS_TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IkNFRmpuOUZRLUctLWcyaUZPZDE5RjRpUXhmYyIsImtpZCI6IkNFRmpuOUZRLUctLWcyaUZPZDE5RjRpUXhmYyJ9.eyJpc3MiOiJodHRwczovL3N0YWdlLmlkZW50aXR5LmJvc2NoLmNvbS8iLCJhdWQiOiJodHRwczovL3N0YWdlLmlkZW50aXR5LmJvc2NoLmNvbS9yZXNvdXJjZXMiLCJleHAiOjE2Mzc2ODYxMzIsIm5iZiI6MTYzNzY4MjUzMiwiY2xpZW50X2lkIjoiY2lhbWlkc18zQTVBNDEwMC04Q0E2LTRGRjktOEQwRC1DOEY0NzVGMTVGQzQiLCJzY29wZSI6WyJvcGVuaWQiLCJwcm9maWxlIiwiZW1haWwiXSwic3ViIjoiUy0xLTUtMjEtMzkyMzc0Mjc5NC0zMjQ4MzQxNzk0LTE1ODIwOTA0ODYtMTIxMzUiLCJhdXRoX3RpbWUiOjE2Mzc2Njk4MjUsImlkcCI6ImlkZW50aXR5c2VydmVyIiwiZW1haWwiOiJzbWFydHNpdGVhcHArZGFuaWVsQGdtYWlsLmNvbSIsImp0aSI6IjNhNTM0M2FmZjFiNTVkODhhZDE2MGI1MDI5OWI5NDQwIiwiYW1yIjpbImV4dGVybmFsIl19.IBWVIGztMSLvzdy7wWnt7jt8GRjnTlTZBLK6_AQEpPyCuq5QhT59F_xa44uN4LjETfMjZ6ML3Giz_93HzEOYYLVBV87mYx3AdyK0Mw9QC5mh4LGEOWruX6eZR3yA1Ebl21XLoyBbCjsm3dxsWbx5c7b4trMrMCNG2_JesNHImw4sAQl74UYyGoE_phY2W6gwZTc5TbN7dg3fSJq-UoqTkttdf7eA-vdKtubCB7yR8myK5rv9WZld61b1mbka3ir5BKaOMP4LclZuAR7-OAkOuWZU4oMZnizBfrcuP3LkB4kcAD2KoIqZ9bG4EjmgzAlsjvwOQL5UE77vvY5JAUKFznfthiDsb0GMwCm2bB8wGECL9tSCr2x2orgZ1VaWZV1oqpQ5zyuiW3So9livpCQjTlWKz6IsGUyrP9LwThJOpn06u-ufHS6YhA1PrxSr3TIGqjjfJrJTwhCH0P9Kol_4drYYA8He9PX7xZVXbJib27Rv45K1Ym8b8dQPW_QDMG8SPM_qCB5CCIWYLAcTQMj7zlnyH9EwiVGIG_v77nYoJ_k40BfP2bDdjKDLx9I3fXnbI7HBwwysNM37PM1uKvvn7CHWNRDrr9mGy1gR12MOR5iQqx0N5NIzw1Uhh_eEiyV9RBibmdNDvCVsanNsXqrE6PvmafGOhyE3eI_SDe3sGuI"
fi

ab -l -n 100 -c 10 \
 -T application/json \
 -p payload.json \
 -H "Authorization: Bearer $ACCESS_TOKEN" \
 -H "Accept: application/pdf" \
  https://$STAGE.bosch-refinemysite.com/api/v4/projects/$PROJECT_IDENTIFIER/calendar/export