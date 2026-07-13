import os
from azure.storage.blob import BlobServiceClient
from .logger import logger

# 환경변수에서 blob 연결문자열 불러오기
connection_string = os.getenv("AZURE_STORAGE_CONNECTION_STRING")
container_name = os.getenv("AZURE_STORAGE_CONTAINER_NAME") 

def upload_to_blob(file_path: str) -> str:
    if not connection_string:
        logger.error("[BLOB] AZURE_STORAGE_CONNECTION_STRING is not loaded")
        raise ValueError("Storage connection string not configured.")
    try:
        blob_service_client = BlobServiceClient.from_connection_string(connection_string)
        blob_name = os.path.basename(file_path)
        blob_client = blob_service_client.get_blob_client(container=container_name, blob=blob_name)

        logger.info(f"[BLOB] Uploading {file_path} to {container_name}/{blob_name}")
        with open(file_path, "rb") as data:
            blob_client.upload_blob(data, overwrite=True)

        logger.info(f"[BLOB] Upload successful : {blob_client.url}")
        return blob_client.url
    except Exception as e:
        logger.error(f"[BLOB] Upload failed for {file_path}: {e}", exc_info=True)
        raise