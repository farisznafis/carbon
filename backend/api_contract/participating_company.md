PATCH /api/company
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

Request Body (JSON):
{
    "phoneNumber": "123453789",
    "fax": "1234567890123",
    "email": "info@example.com",
}

Response - Success (HTTP 200)
{
    "statusCode": "OK",
    "company" : {
        "id": "5678",
        "nameJA": "株式会社サンプル",
        "nameFurigana": "カブシキガイシャサンプル",
        "nameEN": "Sample Corporation",
        "email": "info@example.com",
        "phoneNumber": "0312345678",
        "fax": "0312345670",
        "corpNumber": "1234567890123",
        "address": "千代田区千代田1-1"
    }
}

ERROR RESPONSES
{
    "statusCode": "UNAUTHORIZED",
    "message": "この操作を行うにはログインが必要です。"
},
{
    "statusCode": "FORBIDDEN",
    "message": "このリソースにアクセスする権限がありません。"
},
{
    "statusCode": "NOT_FOUND",
    "message": "該当する企業が見つかりませんでした。"
},
{
    "errorCode": "SERVER_ERROR",
    "message": "サーバー内部でエラーが発生しました。"
}
