// Character dataset for the UI kit demo.
// Includes the real mascot art + portrait placeholders for the rest.

const CHARACTERS = [
  {
    id: 'aria',
    name: 'Aria of Ember',
    star: 3,
    image: 'assets/icon-source.png',
    auAvailable: ['#64DCB4'],
    description: 'Şövalye · son ışıklara karşı.',
  },
  { id: 'kael', name: 'Kael Vance', star: 1, bg: 'linear-gradient(160deg,#2b1e3b 0%,#5e3a82 70%,#bdb3ff 130%)', placeholder: 'Kael · 1★' },
  { id: 'mira', name: 'Mira Solace', star: 2, bg: 'linear-gradient(160deg,#1b2a3a 0%,#3a6e95 60%,#7fd5f5 130%)', placeholder: 'Mira · 2★', auAvailable: ['#64DCB4','#BDB3FF'] },
  { id: 'soren', name: 'Soren Drake', star: 3, bg: 'linear-gradient(160deg,#3a1e1e 0%,#7a3030 50%,#ff8a5b 130%)', placeholder: 'Soren · 3★' },
  { id: 'lyra', name: 'Lyra Nightingale', star: 3, bg: 'linear-gradient(160deg,#2d2050 0%,#5d3f8a 60%,#e0b3ff 130%)', placeholder: 'Lyra · 3★', auAvailable: ['#64DCB4'] },
  { id: 'wren', name: 'Wren Ashbourne', star: 1, bg: 'linear-gradient(160deg,#22322a 0%,#3f6b50 60%,#9adabd 130%)', placeholder: 'Wren · 1★' },
  { id: 'cassian', name: 'Cassian Vox', star: 2, bg: 'linear-gradient(160deg,#1f2438 0%,#3a4670 60%,#9aafd8 130%)', placeholder: 'Cassian · 2★' },
  { id: 'thara', name: 'Thara Bloom', star: 3, bg: 'linear-gradient(160deg,#3a2240 0%,#7c3f8c 60%,#f4a8d8 130%)', placeholder: 'Thara · 3★', auAvailable: ['#BDB3FF'] },
  {
    id: 'aria-x-kael',
    name: 'Aria × Kael',
    star: 4,
    fusion: true,
    bg: 'linear-gradient(160deg,#3a1e5e 0%,#7a3aa5 50%,#e0b3ff 130%)',
    placeholder: '4★ Fusion',
  },
  { id: 'mira-x-soren', name: 'Mira × Soren', star: 4, fusion: true, bg: 'linear-gradient(160deg,#3a285a 0%,#8a5e9e 60%,#ffb8a8 130%)', placeholder: '4★ Fusion' },
  { id: 'old-aldwin', name: 'Old Aldwin', star: 1, dismissed: true, bg: 'linear-gradient(160deg,#2a262e 0%,#4a4248 60%,#79667780 130%)', placeholder: 'retired' },
  { id: 'theo', name: 'Theo Marlowe', star: 3, bg: 'linear-gradient(160deg,#1a2438 0%,#3a5278 60%,#8ab8e8 130%)', placeholder: 'Theo · 3★' },
];

const AUS = [
  { id: 'solaris', name: 'Solaris',  color: '#64DCB4', icon: 'wb_sunny', description: 'Brass-and-amber alternate timeline.' },
  { id: 'eclipse', name: 'Eclipse',  color: '#BDB3FF', icon: 'dark_mode', description: 'Moonlit shadow variant.' },
  { id: 'ember',   name: 'Ember',    color: '#fb923c', icon: 'local_fire_department', description: 'Volcanic timeline.', locked: true },
];

const FILTER_OPTIONS = [
  { id: 'all',    label: 'All',      icon: 'apps' },
  { id: '1-star', label: '1 Star',   icon: 'filter_1', color: '#fb923c' },
  { id: '2-star', label: '2 Star',   icon: 'filter_2', color: '#94a3b8' },
  { id: '3-star', label: '3 Star',   icon: 'filter_3', color: '#fbbf24' },
  { id: 'fusion', label: '4 Star',   icon: 'filter_4', color: '#e879f9' },
  { id: 'dismissed', label: 'Retired', icon: 'badge' },
];

const SORT_OPTIONS = [
  { id: 'name',      label: 'Name (A–Z)', icon: 'sort_by_alpha' },
  { id: 'date_desc', label: 'Newest',     icon: 'arrow_downward' },
  { id: 'date_asc',  label: 'Oldest',     icon: 'arrow_upward' },
];

Object.assign(window, { CHARACTERS, AUS, FILTER_OPTIONS, SORT_OPTIONS });
