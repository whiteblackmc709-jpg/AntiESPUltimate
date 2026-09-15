# AntiESPUltimate

Gộp 3 module phòng thủ vào một plugin:

1. **Block obfuscation** — thay ore + mọi loại container (chest, ender chest,
   barrel, furnace, shulker box, dispenser, dropper, hopper...) bằng block giả
   trong gói chunk gửi đi. Chỉ hiện block thật cho người chơi nào thật sự lại
   gần hoặc có line-of-sight hợp lệ. Đây là phần trực tiếp vô hiệu hoá
   **StorageESP/OreESP/cave-finder** vì client hack chỉ nhận được dữ liệu giả.
2. **Player visibility** — ẩn người chơi khỏi nhau khi bị block chắn tầm nhìn
   (raycast), dùng `Player#hidePlayer`/`showPlayer` chuẩn của Bukkit thay vì tự
   dựng reflection NMS, nên đỡ gãy khi lên version mới.
3. **View distance limiter** — giảm view-distance gửi cho từng người chơi
   (Paper API), giới hạn phạm vi dữ liệu client từng nhận được.

## Yêu cầu

- Paper 1.21.4+ (Paper-only — module view-distance-limit cần `setSendViewDistance`,
  không chạy trên Spigot thuần)
- ProtocolLib 5.4.0+ (bắt buộc, `depend` trong plugin.yml)
- PlaceholderAPI (tùy chọn, `softdepend` — chỉ để expose vài placeholder)

## Lệnh

| Lệnh                  | Tác dụng                                    |
|-----------------------|----------------------------------------------|
| `/antiespu status`    | Trạng thái tổng quan, tóm tắt 1 dòng/module   |
| `/antiespu modules`   | Chi tiết 3 module đang bật/tắt                |
| `/antiespu counters`  | Counter chẩn đoán packet/NBT/reflection       |
| `/antiespu reload`    | Reload config (`config.yml` + tái tạo module) |
| `/antiespu help`      | Hiện lại danh sách lệnh này                   |

Alias: `/aeu`. Permission mặc định: `antiespu.admin` (đổi được qua
`permissions.admin` trong `config.yml`).

## Permission

- `antiespu.admin`   — dùng lệnh `/antiespu`
- `antiespu.bypass`  — thấy mọi thứ thật, không bị obfuscate/ẩn (dành cho staff)

## Placeholder (cần PlaceholderAPI)

Nếu server có PlaceholderAPI, plugin tự hook và expose:

- `%antiespu_enabled%` — plugin có đang bật (`1`/`0`)
- `%antiespu_obf%` / `%antiespu_vis%` / `%antiespu_viewdistance%` — từng
  module có đang chạy
- `%antiespu_packets%` — số MAP_CHUNK packet đã xử lý
- `%antiespu_stripped%` — số block-entity đã bị strip

## bStats

Đặt `bstats-plugin-id` trong `config.yml` (mặc định `-1` = tắt) thành ID thật
lấy từ bstats.org sau khi đăng ký plugin của bạn để bật thống kê usage.

## Về FreeCam — đọc kỹ trước khi kỳ vọng

**Không có plugin server nào chặn được FreeCam**, kể cả plugin này. FreeCam chỉ
tách camera hiển thị ra khỏi nhân vật ở phía client, không gửi thêm gói tin
gì bất thường lên server — nhân vật vẫn "đứng yên" theo dữ liệu server nhận
được. Server không có cơ sở nào để phân biệt "đang dùng freecam" với "đang
đứng yên nhìn quanh" bình thường.

Việc `view-distance-limit` làm được chỉ là giảm **phạm vi dữ liệu** mà server
từng gửi cho client — tức là freecam có bay xa cỡ nào cũng không "nhìn" được
xa hơn những gì server đã gửi. Kết hợp với block-obfuscation, ngay cả khi
FreeCam bay xuyên tường nhìn vào một khu vực đã tải, nó cũng chỉ thấy đá giả
thay vì rương/quặng thật cho tới khi có ai đó thật sự tiếp cận hợp lệ.

Nếu cần một lớp phòng thủ nữa, cân nhắc bổ sung (không có trong code này):
- Cảnh báo admin khi người chơi đứng yên bất thường lâu trong khi hướng nhìn
  liên tục đổi về phía các vị trí có giá trị (heuristic, không chắc chắn).
- Giới hạn `view-distance`/`simulation-distance` toàn server trong
  `server.properties` như một lớp phòng thủ song song.

## Giới hạn đã biết

- Reveal 1 chiều cho container: player từng thấy chest thì không bị re-hide
  khi đi ra xa (tránh desync); ore thì có re-obfuscate khi chunk gửi lại.
- Không chặn được FreeCam bản thân nó (đọc mục trên).
- Chỉ chặn ESP đọc từ MAP_CHUNK + BLOCK_CHANGE; không chặn ESP đọc qua các
  packet khác nếu client hack có cách khác để lấy dữ liệu thế giới.
- `revealTick()` stagger theo player (1 player/tick) để tránh spike CPU, nên
  với rất nhiều player online, thời gian một người "earn" reveal có thể chậm
  hơn vài tick so với bản gốc — đánh đổi hợp lý cho TPS ổn định.

## Build

### Cách 1 — GitHub Actions (không cần cài gì trên máy)

Project đã có sẵn `.github/workflows/build.yml`. Các bước:

1. Tạo một repo GitHub mới (public hoặc private đều được).
2. Đẩy toàn bộ nội dung thư mục `AntiESPUltimate/` (giữ nguyên cấu trúc,
   kể cả thư mục ẩn `.github/`) lên repo đó:
   ```bash
   cd AntiESPUltimate
   git init
   git add .
   git commit -m "initial commit"
   git branch -M main
   git remote add origin <URL repo GitHub của bạn>
   git push -u origin main
   ```
3. Vào tab **Actions** trên GitHub repo → sẽ thấy workflow "Build plugin jar"
   tự chạy sau khi push (mất khoảng 1-2 phút).
4. Khi chạy xong (dấu tích xanh), bấm vào lần chạy đó → kéo xuống mục
   **Artifacts** → tải file `AntiESPUltimate-jar.zip` về. Trong đó chính là
   `AntiESPUltimate.jar` bạn cần.
5. Nếu không muốn đợi push code, vào tab Actions → chọn workflow → bấm
   **Run workflow** để chạy thủ công bất cứ lúc nào.

### Cách 2 — Build trên máy có Maven + JDK 21

```bash
mvn clean package
```

File `target/AntiESPUltimate.jar` sinh ra (đã shade bStats bên trong), bỏ vào
thư mục `plugins/` cùng `ProtocolLib.jar` (bắt buộc phải cài, xem `plugin.yml`).

`mvn test` chạy bộ unit test cho `CoordCodec` (pack/unpack tọa độ round-trip,
kể cả tọa độ âm và biên world/y-limit) — logic thuần duy nhất tách được khỏi
Bukkit API nên test được ngoài môi trường server thật.

## Trước khi đưa lên server thật

- Test trên server dev, không phải production, ít nhất vài ngày.
- `block-obfuscation` viết lại toàn bộ block data trong MỌI gói chunk gửi đi —
  cân nhắc tác động hiệu năng trên server đông người. `revealTick` đã được
  stagger theo player (1 player/tick) từ v1.1 để giảm tải; nếu vẫn lag, tăng
  `reveal-check-interval` hoặc giảm `reveal-distance`.
- Đây vẫn là bản minh hoạ kỹ thuật đã vá các lỗi nghiêm trọng/mức cao đã biết
  (xem lịch sử patch P0-P3), nhưng chưa xử lý mọi edge case tuyệt đối. Test kỹ
  trước khi tin tưởng hoàn toàn, đặc biệt `/antiespu reload` liên tục và
  server đông người trong thời gian dài.
