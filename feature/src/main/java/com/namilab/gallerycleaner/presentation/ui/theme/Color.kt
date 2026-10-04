package com.namilab.gallerycleaner.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// 브랜드 프라이머리 — 결국 이 앱의 핵심은 "클리닝". 맨 처음 틸(#0D9488)을 버린 이유가
// 청소 앱 색이라서가 아니라 탁하고 어두운 톤이었기 때문일 수 있다는 판단으로, 청소/클린이
// 연상되는 블루-시안 계열로 복귀하되 훨씬 밝고 선명한 "맑은 물" 톤으로 재해석했다.
val BrandFresh = Color(0xFF13B6EC)
val BrandFreshDark = Color(0xFF51C5EC)
// 경고·성공 색도 같은 채도·명도 대역으로 통일.
val iOSRed = Color(0xFFE47867)
val iOSRedDark = Color(0xFFEB9284)
val iOSOrange = Color(0xFFEFC16C)
val iOSOrangeDark = Color(0xFFF3CD86)
// 온보딩 피처 아이콘에서 프라이머리와 나란히 쓰이므로 확실한 그린으로 구분하되, 채도는 낮춰
// 같은 파스텔 계열로 맞췄다.
val iOSGreen = Color(0xFF74C464)
val iOSGreenDark = Color(0xFF8DD27F)

// Backgrounds (Light) — 차가운 iOS 그레이 대신 웜 페이퍼 — 피치덱 팔레트를 실제 앱으로 확장
val iOSGroupedBg = Color(0xFFF5F1E8)
val iOSSecondaryGroupedBg = Color(0xFFFFFCF6)
val iOSTertiaryGroupedBg = Color(0xFFEFE8D8)

// Backgrounds (Dark) — 순수 블랙 대신 웜 잉크
val iOSGroupedBgDark = Color(0xFF17130F)
val iOSSecondaryGroupedBgDark = Color(0xFF221C16)
val iOSTertiaryGroupedBgDark = Color(0xFF2C241B)

// Labels — 순수 블랙/화이트 대신 웜 잉크·웜 페이퍼로, 배경 톤과 한 계열
val iOSLabel = Color(0xFF1C1712)
val iOSLabelDark = Color(0xFFF2ECE0)
val iOSSecondaryLabel = Color(0xFF6B6152)
val iOSSecondaryLabelDark = Color(0xFFB8AB96)

// Separators
val iOSSeparator = Color(0xFFD8CDB4)
val iOSSeparatorDark = Color(0xFF3A3025)
val iOSSeparatorOpaque = Color(0xFFE6DDC8)
val iOSSeparatorOpaqueDark = Color(0xFF332B21)

// Gold / Winner accent — 프리미엄 스타 아이콘, 이상형 월드컵 우승 배지. iOSOrange(즐겨찾기 별)와
// 같은 파스텔 골드 톤을 공유해 "특별함=골드" 언어를 통일한다.
val GoldAccent = Color(0xFFEFC16C)

// Premium 카드 — Material 기본 보라색 그라데이션 → 잉크에서 브라운으로 번지는 그라데이션.
// 골드 별·체크와 짝을 이뤄 대결 우승 배지와 같은 "특별함=골드" 언어를 공유한다.
val PremiumGradientStart = Color(0xFF1C1712)
val PremiumGradientEnd = Color(0xFF3D2E1E)
val PremiumCheckGold = Color(0xFFEFC16C)

// Debug 전용 경고 배너 (개발자 옵션)
val DebugWarningBg = Color(0xFFFFF3E0)
val DebugWarningTitle = Color(0xFFE65100)
val DebugWarningSubtitle = Color(0xFFBF360C)
