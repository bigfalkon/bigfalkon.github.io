// Gallery screen — the flagship view of Karakter Evreni.
// Sticky glass control panel + character grid + dropdown menus.

const { useState, useRef, useEffect } = React;

function FilterMenu({ open, current, onPick, onClose, sort, onSort }) {
  if (!open) return null;
  return (
    <div
      className="glass"
      style={{
        position: 'absolute', top: '100%', left: 0, marginTop: 8,
        width: 230, padding: 8, zIndex: 50,
        background: 'var(--surface-high)',
        borderRadius: 14,
      }}
    >
      {FILTER_OPTIONS.map(f => (
        <div
          key={f.id}
          onClick={() => { onPick(f.id); onClose(); }}
          style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '7px 10px', borderRadius: 8, cursor: 'pointer',
            fontSize: 14, fontWeight: 500,
            background: current === f.id ? 'rgba(255,255,255,0.05)' : 'transparent',
          }}
          onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.08)'}
          onMouseLeave={e => e.currentTarget.style.background = current === f.id ? 'rgba(255,255,255,0.05)' : 'transparent'}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <MI name={f.icon} size={20} color={f.color} />
            <span>{f.label}</span>
          </div>
          {current === f.id && <MI name="check" size={18} color="var(--primary)" />}
        </div>
      ))}
      <hr style={{ border: 'none', borderTop: '1px solid var(--outline-var)', margin: '6px 0' }} />
      <div style={{
        padding: '4px 10px', fontSize: 11, fontWeight: 700, textTransform: 'uppercase',
        letterSpacing: '.06em', color: 'var(--on-surface-var)',
      }}>Sort</div>
      {SORT_OPTIONS.map(s => (
        <div
          key={s.id}
          onClick={() => onSort(s.id)}
          style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '7px 10px', borderRadius: 8, cursor: 'pointer',
            fontSize: 14, fontWeight: 500,
          }}
          onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.08)'}
          onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <MI name={s.icon} size={20} />
            <span>{s.label}</span>
          </div>
          {sort === s.id && <MI name="check" size={18} color="var(--primary)" />}
        </div>
      ))}
    </div>
  );
}

function AuMenu({ open, current, onPick, onClose }) {
  if (!open) return null;
  return (
    <div
      style={{
        position: 'absolute', top: '100%', left: 0, marginTop: 8,
        width: 220, padding: 8, zIndex: 50,
        background: 'var(--surface-high)',
        border: '1px solid var(--outline-var)', borderRadius: 14,
      }}
    >
      <div
        onClick={() => { onPick(null); onClose(); }}
        style={{
          display: 'flex', alignItems: 'center', gap: 8,
          padding: '7px 10px', borderRadius: 8, cursor: 'pointer',
          fontSize: 14, fontWeight: 500,
          background: !current ? 'rgba(var(--au-rgb),0.10)' : 'transparent',
          color: !current ? 'var(--au)' : 'inherit',
        }}
      >
        <div style={{
          width: 10, height: 10, borderRadius: '50%',
          border: '1px solid var(--outline-var)', background: 'transparent',
        }} />
        <span style={{ flex: 1 }}>None</span>
        {!current && <MI name="check" size={16} color="var(--au)" />}
      </div>
      {AUS.filter(au => !au.locked).map(au => {
        const sel = current === au.id;
        return (
          <div
            key={au.id}
            onClick={() => { onPick(au.id); onClose(); }}
            style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '7px 10px', borderRadius: 8, cursor: 'pointer',
              fontSize: 14, fontWeight: 500,
              background: sel ? `${au.color}26` : 'transparent',
              color: sel ? au.color : 'inherit',
            }}
          >
            <div style={{ width: 10, height: 10, borderRadius: '50%', background: au.color, border: '1px solid rgba(255,255,255,0.2)' }} />
            <MI name={au.icon} size={16} color={au.color} />
            <span style={{ flex: 1 }}>{au.name}</span>
            {sel && <MI name="check" size={16} color={au.color} />}
          </div>
        );
      })}
    </div>
  );
}

function ControlPanel({ filter, setFilter, sort, setSort, au, setAu, zoom, setZoom }) {
  const [filterOpen, setFilterOpen] = useState(false);
  const [auOpen, setAuOpen] = useState(false);

  const currentFilter = FILTER_OPTIONS.find(f => f.id === filter) || FILTER_OPTIONS[0];
  const activeAu = AUS.find(a => a.id === au);

  const ZOOM_LEVELS = ['XS','S','M','L','XL'];

  return (
    <div className="control-panel glass" style={{ margin: '8px 0 24px' }}>
      <div style={{ position: 'relative' }}>
        <Chip
          icon="search"
          style={{ padding: '8px 12px' }}
        />
      </div>

      <div style={{ position: 'relative' }}>
        <Chip
          selected
          onClick={() => { setFilterOpen(o => !o); setAuOpen(false); }}
          icon={currentFilter.icon}
          style={{ padding: '8px 14px', gap: 6 }}
        >
          {currentFilter.label}
        </Chip>
        <FilterMenu
          open={filterOpen}
          current={filter}
          onPick={setFilter}
          onClose={() => setFilterOpen(false)}
          sort={sort}
          onSort={setSort}
        />
      </div>

      <div style={{ position: 'relative' }}>
        <Chip
          auSelected={!!au}
          onClick={() => { setAuOpen(o => !o); setFilterOpen(false); }}
          style={{
            padding: '8px 14px', gap: 6,
            ...(au ? { borderColor: activeAu.color + '99', color: activeAu.color } : {}),
          }}
        >
          <MI name={activeAu?.icon || 'auto_fix_high'} size={18} color={au ? activeAu.color : undefined} />
          {au ? activeAu.name : 'AU'}
        </Chip>
        <AuMenu
          open={auOpen}
          current={au}
          onPick={setAu}
          onClose={() => setAuOpen(false)}
        />
      </div>

      <div className="divider" />

      <div className="zoom-cluster">
        <button onClick={() => setZoom(z => Math.max(0, z - 1))}>
          <MI name="zoom_out" size={20} />
        </button>
        <span className="label">{ZOOM_LEVELS[zoom]}</span>
        <button onClick={() => setZoom(z => Math.min(4, z + 1))}>
          <MI name="zoom_in" size={20} />
        </button>
      </div>

      <Chip icon="auto_awesome_mosaic" style={{ padding: '8px 14px', gap: 6 }}>Fit</Chip>
    </div>
  );
}

function AuBanner({ au, onExit }) {
  if (!au) return null;
  const def = AUS.find(a => a.id === au);
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      background: `linear-gradient(90deg, ${def.color}33 0%, ${def.color}10 100%)`,
      border: `1px solid ${def.color}66`,
      borderRadius: 12,
      padding: '8px 16px',
      marginBottom: 16,
      fontSize: 14, fontWeight: 600,
      color: def.color,
      backdropFilter: 'blur(8px)',
    }}>
      <MI name="auto_fix_high" size={18} color={def.color} />
      <span>Viewing: <strong style={{ fontFamily: "'Metamorphous', serif" }}>{def.name}</strong> Universe</span>
      <button
        onClick={onExit}
        style={{
          marginLeft: 'auto',
          display: 'inline-flex', alignItems: 'center', gap: 4,
          fontSize: 11, fontWeight: 700, padding: '4px 12px',
          borderRadius: 99,
          border: `1px solid currentColor`,
          background: 'transparent',
          color: 'inherit',
          opacity: 0.85,
          cursor: 'pointer',
        }}
      >
        <MI name="close" size={14} />Exit
      </button>
    </div>
  );
}

function GalleryGrid({ characters, onPick, zoom, au }) {
  const minCardWidths = [180, 200, 220, 240, 260];
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: `repeat(auto-fill, minmax(${minCardWidths[zoom]}px, 1fr))`,
      gap: 20,
      paddingBottom: 60,
    }}>
      {characters.map((c, i) => {
        const auOverride = au ? { au: AUS.find(a => a.id === au)?.name, auColor: AUS.find(a => a.id === au)?.color } : {};
        return (
          <CharacterCard
            key={c.id}
            character={{ ...c, ...auOverride }}
            onClick={() => onPick(c)}
            style={{ animationDelay: `${i * 35}ms` }}
          />
        );
      })}
    </div>
  );
}

function GalleryScreen({ onPickCharacter }) {
  const [filter, setFilter] = useState('all');
  const [sort, setSort] = useState('name');
  const [au, setAu] = useState(null);
  const [zoom, setZoom] = useState(2);

  useEffect(() => {
    document.body.classList.toggle('au-mode-active', !!au);
  }, [au]);

  const filtered = CHARACTERS.filter(c => {
    if (filter === 'all') return !c.dismissed;
    if (filter === 'dismissed') return c.dismissed;
    if (filter === 'fusion') return c.fusion;
    if (filter === '1-star') return c.star === 1 && !c.fusion;
    if (filter === '2-star') return c.star === 2 && !c.fusion;
    if (filter === '3-star') return c.star === 3 && !c.fusion;
    return true;
  });

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', padding: '24px 28px 60px' }}>
      <header style={{ marginBottom: 18 }}>
        <h1 style={{
          fontSize: 'clamp(2rem, 4vw, 2.5rem)',
          fontFamily: "'Metamorphous', serif",
          color: 'var(--primary)',
          fontWeight: 800,
          margin: 0,
          lineHeight: 1.05,
        }}>Character Gallery</h1>
        <p style={{
          margin: '4px 0 0',
          color: 'var(--on-surface-var)',
          fontSize: 15,
        }}>Discover the Heroes of the Universe</p>
      </header>

      <AuBanner au={au} onExit={() => setAu(null)} />

      <ControlPanel
        filter={filter} setFilter={setFilter}
        sort={sort} setSort={setSort}
        au={au} setAu={setAu}
        zoom={zoom} setZoom={setZoom}
      />

      <GalleryGrid characters={filtered} onPick={onPickCharacter} zoom={zoom} au={au} />
    </div>
  );
}

Object.assign(window, { GalleryScreen });
