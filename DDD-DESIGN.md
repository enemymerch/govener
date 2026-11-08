# Govener — DDD Tasarım Kararları

Son güncelleme: 8 Kasım 2026

Bu doküman, apartman/site yönetim uygulaması için görüşmelerde alınan kararları kaydeder. **Kesinleşen kararlar** ile **taslak öneriler** birbirinden ayrıdır. Taslaklar, geliştirme için onaylanmış gereksinim sayılmaz. Qwen ile geliştirmeye geçildiğinde açık konular varsayımla iş kuralına dönüştürülmemelidir.

## 1. Ürün ve teknik kapsam — kesinleşen

- Hizmet başlangıçta Türkiye, İstanbul için sunulacak.
- Hedef kullanıcılar bağımsız apartman/site yönetimleri ve profesyonel yönetim firmalarıdır.
- Ürün sahibi kendi firmasıyla yönetim hizmeti verirken uygulamayı kullanabilecek; müşteriler yalnızca uygulamayı da satın alabilecek.
- Backend Java Spring Boot ile monolith olacak.
- Veritabanı MongoDB olacak.
- Kullanıcı uygulamasının web ve Android sürümleri olacak.
- İlk geliştirilecek ürün, yönetici ve sakinin ortak kullandığı uygulamadır.
- Admin frontend'i ayrı olacak; admin gereksinimleri ve geliştirmesi sonraya bırakılmıştır.
- Yönetici telefondan da yönetim işlemleri yapabilecek. Android'in ayrıntılı işlem kapsamı henüz kesinleşmedi.
- İlk sürümde manuel havale/EFT takibi yeterlidir. Online ödeme sonraki sürümlere bırakılmıştır.
- Yönetim firmasının kendi muhasebesi ilk sürüme dahil değildir. Apartman/site mali takibi bundan ayrı bir alandır.

## 2. Ortak dil ve roller — kesinleşen

| Terim | Tanım |
|---|---|
| Apartman/Site Admini | Yetkili olduğu apartman/siteyle ilgili bütün işlemleri yapabilen rol. Ayrı frontend kullanır. |
| Apartman/Site Yöneticisi | Ortak kullanıcı uygulamasını kullanan, sınırlı yönetim yetkilerine sahip rol. |
| Sakin | Yöneticiyle aynı uygulamayı kullanan, sınırlı erişime sahip rol. |
| Bağımsız bölüm | Hesabı takip edilen daire, dükkân veya ofis gibi birim. Görüşmelerdeki “daire bazlı hesap” bu kavram üzerinden modellenir. |
| Malik/kiracı ilişkisi | Bir kişinin bağımsız bölümle tarihli ilişkisi. Yönetim yetkisinden ayrıdır. |

Yönetici ve sakin aynı frontend'e giriş yapar; erişimleri farklıdır. Ayrıntılı yetki matrisi henüz kesinleşmemiştir. “Yönetici” rolü sınırsız erişim anlamına gelmez.

## 3. Temel domain kararları — açıkça onaylanan

### DDD-001 — Bağımsız bölüm bazlı borç hesabı

Borç hesabı kişiye değil bağımsız bölüme bağlı takip edilir.

Sonuçları:

- Malik veya kiracı değiştiğinde bağımsız bölümün hesabı ve geçmişi korunur.
- Borcun bağlı olduğu hesap ile borcun ilgili kişileri ayrı kavramlardır.
- Bu teknik takip modeli, tek başına hukuki ödeme sorumluluğu belirlemez.
- Eski borcun yeni kişiye yüklenmesi veya gösterilmesi için henüz kesinleşmiş bir kural yoktur.

### DDD-002 — Tarihli malik/kiracı ilişkileri

Malik ve kiracı ilişkileri başlangıç ve bitiş tarihleriyle tutulur.

Sonuçları:

- Kişi değişiminde eski ilişki silinmez; bitirilir ve yeni ilişki oluşturulur.
- Geçmiş ilişkiler korunur.
- Geçmiş ilişkilerin korunması, eski kullanıcının erişiminin süresiz devam edeceği anlamına gelmez.
- Dönem sınırları, ay ortasında değişiklik ve geçmişe dönük düzeltme kuralları ayrıca belirlenecektir.

### DDD-003 — Yönetim yetkisi ile daire ilişkisinin ayrılması

Yönetim yetkileri malik/kiracı ilişkisinden bağımsız tutulur.

Sonuçları:

- Yönetici olmak için ilgili sitede malik veya kiracı olmak gerekmez.
- Bir kişi hem yönetici hem sakin olabilir.
- Malik veya kiracı olmak otomatik yönetim yetkisi vermez.
- Yönetim yetkisinin sonlanması mali kayıtları ve daire ilişkilerini silmez.

## 4. Kavramsal model — taslak

Aşağıdaki isimler ve sınırlar görüşmede önerilmiştir; henüz nihai sınıf, aggregate veya koleksiyon tasarımı değildir.

| Kavram | Kod adı önerisi | Sorumluluk |
|---|---|---|
| Yönetim alanı | `Community` | Yönetilen apartman veya site; yönetim firması değildir. |
| Blok | `Block` | Site içindeki isteğe bağlı blok yapısı. |
| Bağımsız bölüm | `Unit` | Daire, dükkân veya ofis ve buna ait hesap bağlamı. |
| Kişi | `Person` | Uygulamaya giriş yapmasa da kaydı bulunabilen kişi. |
| Kullanıcı hesabı | `UserAccount` | Kimlik doğrulama ve uygulamaya giriş hesabı. |
| Daire ilişkisi | `UnitRelationship` | Kişi ile bağımsız bölüm arasındaki tarihli malik/kiracı ilişkisi. |
| Yönetim üyeliği/yetkisi | `CommunityMembership` | Kullanıcının yönetim alanındaki rol ve izinleri. |
| Borç | `Charge` | Bağımsız bölüme çıkarılmış aidat veya ek ödeme. |
| Toplu borçlandırma | `ChargeBatch` | Bir işlemle birden fazla bağımsız bölüme borç çıkarılması. |
| Borçla ilgili kişi bağlantısı | `ChargeParty` | Borç ile ilgili tarihli daire ilişkileri arasında önerilen bağlantı. |
| Ödeme bildirimi | `PaymentNotification` | Sakinin ödeme yaptığını bildirmesi. |
| Tahsilat | `Payment` | Yönetici tarafından doğrulanan para girişi. |
| Ödeme dağılımı | `PaymentAllocation` | Bir tahsilatın borçları hangi tutarlarda kapattığı. |
| Gider | `Expense` | Apartman/site için oluşan gider. |
| Gider ödemesi | `ExpensePayment` | Gider için gerçekleşen para çıkışı. |
| Mali hesap | `FinancialAccount` | Banka hesabı veya nakit kasa. |
| Duyuru | `Announcement` | Belirli bir hedef kitleye yayımlanan içerik. |
| Talep | `ServiceRequest` | Arıza, şikâyet veya hizmet talebi. |

Önerilen ilişkiler:

```text
Community
  ├── Block (isteğe bağlı)
  ├── Unit
  │     ├── UnitRelationship → Person
  │     └── Charge
  └── CommunityMembership → UserAccount

UserAccount → Person
Payment → PaymentAllocation → Charge
PaymentNotification → Payment (onay/eşleştirme sonrasında)
Expense → ExpensePayment → FinancialAccount
```

Bu şema sahiplik veya MongoDB içine gömme kararı değildir; kavramsal ilişkileri gösterir.

## 5. Modül sınırları — taslak

Tek monolith içinde önerilen modüller:

| Modül | Sorumluluk |
|---|---|
| `identity` | Kullanıcı hesapları ve kimlik doğrulama. |
| `community` | Apartman/site, blok, bağımsız bölüm, kişiler ve ilişkiler. |
| `access` | Yönetici/sakin erişimi ve izinler. |
| `finance` | Borçlandırma, tahsilat, dağılım, gider ve mali hesaplar. |
| `communication` | Duyurular ve hedef kitle. |
| `service` | Talepler, atamalar ve durum geçmişi. |
| `notification` | Uygulama içi bildirim ve Android push gönderimi. |

Modül sınırları servis ayrıştırması anlamına gelmez. Kesin aggregate sınırları ve modüller arası sözleşmeler henüz belirlenmemiştir.

## 6. Finans iş kuralları — onay bekleyen öneriler

- Ödeme bildirimi ve tahsilat ayrı kayıtlardır; dekont yüklemek tek başına borcu kapatmaz.
- Yönetici bir bildirimi tahsilata dönüştürebilir veya mevcut tahsilatla eşleştirebilir.
- Bildirim olmadan da manuel tahsilat kaydedilebilir.
- Kısmi ödeme ve fazla ödeme desteklenir.
- Bir tahsilat birden fazla borca dağıtılabilir; ilk sürümde dağılım aynı bağımsız bölüm içinde kalır.
- Dağılım toplamı tahsilatı, bir borca dağıtılan tutar da borcun kalanını aşamaz.
- Dağıtılmayan tutar bağımsız bölümün kullanılabilir alacağı olarak izlenir.
- Borç dönemi, oluşturulma tarihi ve son ödeme tarihi ayrı tutulur.
- Borçların ilgili kişileri yalnızca oluşturulma tarihinden çıkarılmaz; geçmiş dönem için sonradan borç oluşturulabilir.
- Giderin oluşması ile giderin ödenmesi ayrı olaylardır.
- Mali kayıtlar sessizce silinmez; düzeltme ve geri alma işlemleri izlenebilir olur.
- Tekrarlanan istekler çift borç veya çift tahsilat üretmez.

Önerilen ödeme bildirimi durumları: `PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`.

Ödeme dağıtım sırası, gecikme hesabı, iptal/ters kayıt mekanizması ve ilgili kişinin nasıl belirleneceği henüz karara bağlanmamıştır.

## 7. Görünürlük — açık karar

Son görüşmede aşağıdaki yaklaşım önerildi ancak henüz kullanıcı tarafından onaylanmadı:

- Kiracı yalnızca kendisiyle ilişkilendirilmiş borç ve ödemeleri görür.
- Malik kendi mülkiyet dönemine ait daire hesabını görür.
- Yönetici yetkisi kapsamında dairenin bütün mali geçmişini görür.
- Yeni kiracı eski borcun ilgili kişisi olarak otomatik atanmaz.

Şu konular netleştirilmeden bu öneri uygulanmış kural sayılmamalıdır:

1. Eski borçlar yeni sakin ekranında hangi biçimde gösterilecek veya gizlenecek?
2. Daire toplam bakiyesi ile kişinin görebildiği işlemlerden oluşan bakiye nasıl ayrılacak?
3. İlişkisi biten kişinin geçmişe erişimi ne kadar ve hangi kapsamda devam edecek?
4. Malik ve kiracının aynı dönemdeki erişimleri nasıl farklılaşacak?
5. Ödemedeki gönderen bilgisi ve dekont gibi kişisel belgeleri kim görebilecek?

## 8. Teknik tasarım ilkeleri — öneri

- DDD kavramları doğrudan MongoDB koleksiyonlarına eşitlenmez.
- Bütün site, daireler ve mali geçmiş tek bir büyüyen dokümana gömülmez.
- Yönetim alanına ait işlemlerde `communityId` ve sunucu tarafında yetki kontrolü kullanılır.
- Farklı yönetim alanlarındaki daire, borç ve mali hesapların aynı işleme bağlanması engellenir.
- Para hesabında `double` kullanılmaz; uygun ondalık temsil ve para birimi birlikte kullanılır.
- Tahsilat, dağılım ve hesap hareketlerinin tutarlılığı birlikte ele alınır; MongoDB transaction ve eşzamanlılık tasarımı buna göre yapılır.
- Mali işlemlerde yapan kişi, zaman ve gerekçe kaydedilir.
- İşlem anları ile dönem/son ödeme günü gibi takvim tarihleri ayrılır; kullanıcı gösteriminde Türkiye saat dilimi dikkate alınır.
- Frontend'lerin farklı olması yetkilendirme yerine geçmez.

## 9. Sonraki domain çalışmaları

1. Borç ve ödeme görünürlük kurallarını kesinleştirmek.
2. Yönetici ve sakin için işlem bazlı yetki matrisini çıkarmak.
3. Borçlandırma, kısmi/fazla ödeme, taşınma ve düzeltme senaryolarını örneklerle tanımlamak.
4. Aggregate sınırlarını, invariant'ları ve transaction sınırlarını belirlemek.
5. Domain event ve uygulama komutlarını tanımlamak.
6. MongoDB fiziksel modelini ve indeksleri tasarlamak.
7. API sözleşmesi ve kabul kriterleriyle Qwen geliştirme görevlerini hazırlamak.

Admin paneli, abonelik/fiyatlandırma, yönetim firması muhasebesi ve online ödeme bu çalışma için mevcut geliştirme kapsamının dışındadır.
