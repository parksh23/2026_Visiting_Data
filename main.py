from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
import httpx
import os
from dotenv import load_dotenv
from pydantic import BaseModel

load_dotenv()

app = FastAPI(title="Busan Local Tour API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 개발 단계에서는 전체 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

TOUR_API_KEY = os.getenv("TOUR_API_KEY")
TOUR_API_BASE_URL = "https://apis.data.go.kr/B551011/KorService2"
PHOTO_API_BASE_URL = "https://apis.data.go.kr/B551011/PhotoGalleryService1"

BUSAN_DISTRICTS = [
    "중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구",
    "해운대구", "사하구", "금정구", "강서구", "연제구", "수영구", "사상구", "기장군"
]
BUSAN_SIGUNGU_CODES = {"강서구": 1, "금정구": 2, "기장군": 3, "남구": 4, 
                       "동구": 5, "동래구": 6, "부산진구": 7, "북구": 8, 
                       "사상구": 9, "사하구": 10, "서구": 11,  "수영구": 12, 
                       "연제구": 13, "영도구": 14, "중구": 15, "해운대구": 16}
FAVORITES = []
favorite_id_counter = 1
class FavoriteRequest(BaseModel):
    user_id: int
    content_id: str
    content_type_id: str | None = None
    title: str
    address: str | None = None
    image: str | None = None
    lat: str | None = None
    lng: str | None = None
    district: str | None = None

@app.get("/")
def root():
    return {"message": "부산 로컬 관광 서비스 백엔드 실행 중"}


@app.get("/health")
def health_check():
    return {"status": "ok"}


@app.get("/regions")
def get_regions():
    return {
        "city": "부산광역시",
        "districts": BUSAN_DISTRICTS
    }


@app.get("/tour/search")
async def search_tour(
    keyword: str = Query(..., description="검색어"),
    page: int = Query(1, description="페이지 번호"),
    size: int = Query(10, description="한 페이지 결과 수")
):
    if not TOUR_API_KEY:
        return {
            "error": "TOUR_API_KEY가 설정되지 않았습니다.",
            "message": ".env 파일에 TOUR_API_KEY를 입력해주세요."
        }

    params = {
        "serviceKey": TOUR_API_KEY,
        "MobileOS": "ETC",
        "MobileApp": "BusanLocalTour",
        "_type": "json",
        "numOfRows": size,
        "pageNo": page,
        "keyword": keyword,
        "areaCode": 6
    }

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{TOUR_API_BASE_URL}/searchKeyword2",
            params=params
        )

    data = response.json()

    try:
        items = data["response"]["body"]["items"]["item"]
    except (KeyError, TypeError):
        return {
            "keyword": keyword,
            "count": 0,
            "items": []
        }

    if isinstance(items, dict):
        items = [items]

    results = []

    for item in items:
        results.append({
            "content_id": item.get("contentid"),
            "content_type_id": item.get("contenttypeid"),
            "title": item.get("title"),
            "address": item.get("addr1"),
            "image": item.get("firstimage"),
            "thumbnail": item.get("firstimage2"),
            "lat": item.get("mapy"),
            "lng": item.get("mapx"),
            "sigungu_code": item.get("sigungucode")
        })

    return {
        "keyword": keyword,
        "count": len(results),
        "items": results
    }
@app.get("/tour/detail/{content_id}")
async def get_tour_detail(content_id: str):
    if not TOUR_API_KEY:
        return {
            "error": "TOUR_API_KEY가 설정되지 않았습니다.",
            "message": ".env 파일에 TOUR_API_KEY를 입력해주세요."
        }

    params = {
        "serviceKey": TOUR_API_KEY,
        "MobileOS": "ETC",
        "MobileApp": "BusanLocalTour",
        "_type": "json",
        "contentId": content_id
    }

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{TOUR_API_BASE_URL}/detailCommon2",
            params=params
        )

    data = response.json()

    header = data.get("response", {}).get("header", {})
    body = data.get("response", {}).get("body", {})
    items = body.get("items", {})

    if not items or "item" not in items:
        return {
            "content_id": content_id,
            "detail": None,
            "result_code": header.get("resultCode"),
            "result_msg": header.get("resultMsg"),
            "total_count": body.get("totalCount"),
            "raw": data
        }

    item = items["item"]

    if isinstance(item, list):
        item = item[0]

    return {
        "content_id": item.get("contentid"),
        "content_type_id": item.get("contenttypeid"),
        "title": item.get("title"),
        "address": item.get("addr1"),
        "address_detail": item.get("addr2"),
        "tel": item.get("tel"),
        "homepage": item.get("homepage"),
        "overview": item.get("overview"),
        "image": item.get("firstimage"),
        "thumbnail": item.get("firstimage2"),
        "lat": item.get("mapy"),
        "lng": item.get("mapx"),
        "area_code": item.get("areacode"),
        "sigungu_code": item.get("sigungucode")
    }

@app.get("/tour/region/{district}")
async def get_tour_by_region(
    district: str,
    page: int = Query(1, description="페이지 번호"),
    size: int = Query(10, description="한 페이지 결과 수")
):
    if not TOUR_API_KEY:
        return {
            "error": "TOUR_API_KEY가 설정되지 않았습니다.",
            "message": ".env 파일에 TOUR_API_KEY를 입력해주세요."
        }

    sigungu_code = BUSAN_SIGUNGU_CODES.get(district)

    if sigungu_code is None:
        return {
            "error": "지원하지 않는 구·군입니다.",
            "district": district,
            "available_districts": list(BUSAN_SIGUNGU_CODES.keys())
        }

    params = {
        "serviceKey": TOUR_API_KEY,
        "MobileOS": "ETC",
        "MobileApp": "BusanLocalTour",
        "_type": "json",
        "numOfRows": size,
        "pageNo": page,
        "areaCode": 6,          # 부산광역시
        "sigunguCode": sigungu_code,
        "arrange": "A"
    }

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{TOUR_API_BASE_URL}/areaBasedList2",
            params=params
        )

    data = response.json()

    try:
        items = data["response"]["body"]["items"]["item"]
    except (KeyError, TypeError):
        return {
            "district": district,
            "sigungu_code": sigungu_code,
            "count": 0,
            "items": []
        }

    if isinstance(items, dict):
        items = [items]

    results = []

    for item in items:
        results.append({
            "content_id": item.get("contentid"),
            "content_type_id": item.get("contenttypeid"),
            "title": item.get("title"),
            "address": item.get("addr1"),
            "image": item.get("firstimage"),
            "thumbnail": item.get("firstimage2"),
            "lat": item.get("mapy"),
            "lng": item.get("mapx"),
            "area_code": item.get("areacode"),
            "sigungu_code": item.get("sigungucode")
        })

    return {
        "district": district,
        "sigungu_code": sigungu_code,
        "count": len(results),
        "items": results
    }
@app.get("/photos/search")
async def search_photos(
    keyword: str = Query(..., description="사진 검색어"),
    page: int = Query(1, description="페이지 번호"),
    size: int = Query(10, description="한 페이지 결과 수")
):
    if not TOUR_API_KEY:
        return {
            "error": "TOUR_API_KEY가 설정되지 않았습니다.",
            "message": ".env 파일에 TOUR_API_KEY를 입력해주세요."
        }

    params = {
        "serviceKey": TOUR_API_KEY,
        "MobileOS": "ETC",
        "MobileApp": "BusanLocalTour",
        "_type": "json",
        "numOfRows": size,
        "pageNo": page,
        "arrange": "A",
        "keyword": keyword
    }

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{PHOTO_API_BASE_URL}/gallerySearchList1",
            params=params
        )

    data = response.json()

    try:
        items = data["response"]["body"]["items"]["item"]
    except (KeyError, TypeError):
        return {
            "keyword": keyword,
            "count": 0,
            "items": []
        }

    if isinstance(items, dict):
        items = [items]

    results = []

    for item in items:
        results.append({
            "gal_content_id": item.get("galContentId"),
            "title": item.get("galTitle"),
            "location": item.get("galPhotographyLocation"),
            "photographer": item.get("galPhotographer"),
            "search_keyword": item.get("galSearchKeyword"),
            "image_url": item.get("galWebImageUrl"),
            "created_time": item.get("galCreatedtime"),
            "modified_time": item.get("galModifiedtime")
        })

    return {
        "keyword": keyword,
        "count": len(results),
        "items": results
    }
@app.get("/tour/nearby")
async def get_nearby_tour(
    lat: float = Query(..., description="현재 위치 위도"),
    lng: float = Query(..., description="현재 위치 경도"),
    radius: int = Query(1000, description="검색 반경, 단위 m"),
    page: int = Query(1, description="페이지 번호"),
    size: int = Query(10, description="한 페이지 결과 수")
):
    if not TOUR_API_KEY:
        return {
            "error": "TOUR_API_KEY가 설정되지 않았습니다.",
            "message": ".env 파일에 TOUR_API_KEY를 입력해주세요."
        }

    params = {
        "serviceKey": TOUR_API_KEY,
        "MobileOS": "ETC",
        "MobileApp": "BusanLocalTour",
        "_type": "json",
        "numOfRows": size,
        "pageNo": page,
        "mapX": lng,
        "mapY": lat,
        "radius": radius,
        "arrange": "E"  # 거리순 정렬
    }

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{TOUR_API_BASE_URL}/locationBasedList2",
            params=params
        )

    data = response.json()

    try:
        items = data["response"]["body"]["items"]["item"]
    except (KeyError, TypeError):
        return {
            "lat": lat,
            "lng": lng,
            "radius": radius,
            "count": 0,
            "items": []
        }

    if isinstance(items, dict):
        items = [items]

    results = []

    for item in items:
        results.append({
            "content_id": item.get("contentid"),
            "content_type_id": item.get("contenttypeid"),
            "title": item.get("title"),
            "address": item.get("addr1"),
            "image": item.get("firstimage"),
            "thumbnail": item.get("firstimage2"),
            "lat": item.get("mapy"),
            "lng": item.get("mapx"),
            "distance": item.get("dist"),
            "area_code": item.get("areacode"),
            "sigungu_code": item.get("sigungucode")
        })

    return {
        "lat": lat,
        "lng": lng,
        "radius": radius,
        "count": len(results),
        "items": results
    }

@app.post("/favorites")
def add_favorite(request: FavoriteRequest):
    global favorite_id_counter

    for favorite in FAVORITES:
        if (
            favorite["user_id"] == request.user_id
            and favorite["content_id"] == request.content_id
        ):
            return {
                "message": "이미 찜한 관광지입니다.",
                "favorite": favorite
            }

    new_favorite = {
        "favorite_id": favorite_id_counter,
        "user_id": request.user_id,
        "content_id": request.content_id,
        "content_type_id": request.content_type_id,
        "title": request.title,
        "address": request.address,
        "image": request.image,
        "lat": request.lat,
        "lng": request.lng,
        "district": request.district
    }

    FAVORITES.append(new_favorite)
    favorite_id_counter += 1

    return {
        "message": "찜 추가 완료",
        "favorite": new_favorite
    }
