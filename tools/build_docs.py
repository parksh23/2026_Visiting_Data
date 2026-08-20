# -*- coding: utf-8 -*-
"""
약관·방침 문서를 GitHub Pages 용 HTML 로 변환한다.

앱과 웹이 서로 다른 문구를 갖는 사고를 막으려고, 원본은 한 곳만 둔다.
  · 앱 안에서 보는 문서: android/app/src/main/assets/*.md   (원본)
  · 웹에 공개하는 문서:  docs/*.html                        (이 스크립트가 생성)
  · 계정 삭제 페이지만 웹 전용: tools/pages/delete-account.md

문서를 고칠 때는 .md 만 고치고 이 스크립트를 다시 돌린다.
    python tools/build_docs.py

Play Console 에 넣을 URL
  개인정보처리방침 : https://parksh23.github.io/2026_Visiting_Data/privacy.html
  계정 삭제        : https://parksh23.github.io/2026_Visiting_Data/delete-account.html
"""
import html
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "android", "app", "src", "main", "assets")
PAGES = os.path.join(ROOT, "tools", "pages")
OUT = os.path.join(ROOT, "docs")

APP_NAME = "부산 땅따먹기"
DEVELOPER = "삠삠"
CONTACT = "sihyunb88@gmail.com"

# (출력파일, 원본경로, 내비게이션 라벨)
DOCS = [
    ("privacy.html", os.path.join(ASSETS, "privacy.md"), "개인정보처리방침"),
    ("terms.html", os.path.join(ASSETS, "terms.md"), "이용약관"),
    ("location.html", os.path.join(ASSETS, "location.md"), "위치기반서비스 이용약관"),
    ("delete-account.html", os.path.join(PAGES, "delete-account.md"), "계정 삭제"),
]

# 앱과 같은 팔레트 (웜 페이퍼 + 코럴)
CSS = """
:root{--bg:#F7F3EA;--card:#FFFFFF;--ink:#2B2320;--sub:#766B62;
      --line:#D3C3AA;--divider:#E8DFD1;--coral:#E8635F;--coral-dark:#BC403A;--tint:#FCE8E5}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);line-height:1.75;
     font-family:-apple-system,"Apple SD Gothic Neo","Malgun Gothic","Noto Sans KR",sans-serif}
.wrap{max-width:760px;margin:0 auto;padding:32px 20px 80px}
header{border-bottom:1.5px solid var(--line);padding-bottom:18px;margin-bottom:28px}
.brand{font-size:20px;font-weight:800;letter-spacing:-.02em}
.brand span{color:var(--coral-dark)}
.meta{color:var(--sub);font-size:13px;margin-top:6px}
nav{margin-top:16px;display:flex;flex-wrap:wrap;gap:8px}
nav a{font-size:13px;text-decoration:none;color:var(--ink);background:var(--card);
      border:1.5px solid var(--line);border-radius:999px;padding:6px 13px}
nav a.on{background:var(--tint);border-color:var(--coral-dark);color:var(--coral-dark);font-weight:700}
h1{font-size:26px;margin:0 0 6px}
.doc-meta{color:var(--sub);font-size:13px;margin-bottom:26px}
.card{background:var(--card);border:1.5px solid var(--line);border-radius:18px;padding:26px}
h2{font-size:17px;margin:30px 0 10px;padding-top:4px}
h2:first-child{margin-top:0}
p{margin:10px 0;font-size:15px}
ul{margin:10px 0;padding-left:20px}
li{font-size:15px;margin:5px 0;color:var(--sub)}
.note{background:var(--tint);border-radius:12px;padding:14px 16px;margin:16px 0;
      font-size:14px;color:#4A1B0C}
.cta{display:inline-block;background:var(--coral);color:#fff;font-weight:700;text-decoration:none;
     border:1.5px solid var(--ink);border-radius:14px;padding:13px 22px;margin:6px 0}
footer{margin-top:32px;color:var(--sub);font-size:13px;text-align:center}
a{color:var(--coral-dark)}
""".strip()


def parse(path):
    """DocumentScreen.kt 와 같은 규칙으로 머리말 + 본문을 나눈다."""
    with open(path, encoding="utf-8") as f:
        lines = f.read().replace("\r\n", "\n").split("\n")
    sep = next((i for i, l in enumerate(lines) if l.strip() == "---"), -1)
    head, body = (lines[:sep], lines[sep + 1:]) if sep >= 0 else ([], lines)

    def pick(prefix, default=""):
        for l in head:
            if l.startswith(prefix):
                return l[len(prefix):].strip()
        return default

    return {
        "title": pick("# ", "문서"),
        "version": pick("version:", "-"),
        "effective": pick("effective:", "-"),
        "body": body,
    }


def to_html(body):
    """## 제목 / - 목록 / > 안내 / 그 외 문단. 앱 뷰어와 동일한 문법만 지원."""
    out, in_list = [], False

    def close_list():
        nonlocal in_list
        if in_list:
            out.append("</ul>")
            in_list = False

    for raw in body:
        line = raw.rstrip()
        if not line.strip():
            close_list()
            continue
        if line.startswith("## "):
            close_list()
            out.append(f"<h2>{html.escape(line[3:])}</h2>")
        elif line.startswith("- "):
            if not in_list:
                out.append("<ul>")
                in_list = True
            out.append(f"<li>{linkify(line[2:])}</li>")
        elif line.startswith("> "):
            close_list()
            out.append(f'<div class="note">{linkify(line[2:])}</div>')
        else:
            close_list()
            out.append(f"<p>{linkify(line)}</p>")
    close_list()
    return "\n".join(out)


def linkify(text):
    """메일 주소는 자동으로 mailto 링크로 (계정 삭제 페이지 요구사항)."""
    escaped = html.escape(text)
    return re.sub(
        r"([\w.+-]+@[\w-]+\.[\w.]+)",
        r'<a href="mailto:\1">\1</a>',
        escaped,
    )


def nav(current):
    items = ['<a href="index.html"%s>홈</a>' % (' class="on"' if current == "index.html" else "")]
    for fn, _, label in DOCS:
        on = ' class="on"' if current == fn else ""
        items.append(f'<a href="{fn}"{on}>{label}</a>')
    return "<nav>" + "".join(items) + "</nav>"


def page(current, title, meta_line, inner):
    return f"""<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)} · {APP_NAME}</title>
<meta name="description" content="{html.escape(APP_NAME)} {html.escape(title)}">
<style>{CSS}</style>
</head>
<body>
<div class="wrap">
  <header>
    <div class="brand">부산 <span>땅</span>따먹기</div>
    <div class="meta">개발자 {html.escape(DEVELOPER)} · 문의 <a href="mailto:{CONTACT}">{CONTACT}</a></div>
    {nav(current)}
  </header>
  <h1>{html.escape(title)}</h1>
  <div class="doc-meta">{meta_line}</div>
  <div class="card">
{inner}
  </div>
  <footer>© 2026 {html.escape(DEVELOPER)} · {html.escape(APP_NAME)}</footer>
</div>
</body>
</html>
"""


def build():
    os.makedirs(OUT, exist_ok=True)

    for fn, src, label in DOCS:
        if not os.path.exists(src):
            print(f"  건너뜀 (원본 없음): {src}")
            continue
        d = parse(src)
        meta = f"버전 {d['version']} · 시행일 {d['effective']}"
        with open(os.path.join(OUT, fn), "w", encoding="utf-8") as f:
            f.write(page(fn, d["title"], meta, to_html(d["body"])))
        print(f"  생성: docs/{fn}  ← {os.path.relpath(src, ROOT)}")

    # 홈
    links = "\n".join(
        f'<li><a href="{fn}">{label}</a></li>' for fn, _, label in DOCS
    )
    inner = f"""<h2>{APP_NAME} 안내 페이지</h2>
<p>부산의 관광지를 찾아다니며 미션을 완료하고 구·군을 점령하는 앱입니다.</p>
<div class="note">계정을 삭제하고 싶으신가요? 아래 버튼을 눌러 요청하실 수 있습니다. 앱을 이미 삭제하셨어도 요청 가능합니다.</div>
<p><a class="cta" href="delete-account.html">계정 삭제 요청하기</a></p>
<h2>문서</h2>
<ul>
{links}
</ul>
<h2>문의</h2>
<ul>
<li>개발자: {DEVELOPER}</li>
<li>메일: {CONTACT}</li>
</ul>"""
    with open(os.path.join(OUT, "index.html"), "w", encoding="utf-8") as f:
        f.write(page("index.html", f"{APP_NAME}", "앱 정보 · 약관 · 계정 삭제", inner))
    print("  생성: docs/index.html")


if __name__ == "__main__":
    print("문서 빌드 시작")
    build()
    print("완료. docs/ 를 커밋하고 GitHub Pages 를 켜세요.")
