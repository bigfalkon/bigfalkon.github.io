// Karakter Evreni UI Kit — shared primitives
// Glass panels, chips, icon buttons, character card pieces.

const STAR_COLORS = {
  1: '#fb923c',
  2: '#94a3b8',
  3: '#fbbf24',
  4: '#e879f9',
};

const MI = ({ name, size = 18, fill = false, style = {}, color }) => (
  <span
    className="material-symbols-outlined"
    style={{
      fontSize: size,
      fontVariationSettings: fill ? "'FILL' 1" : "'FILL' 0",
      color,
      ...style,
    }}
  >{name}</span>
);

const Chip = ({ children, icon, selected, auSelected, onClick, style }) => (
  <button
    className={`chip ${selected ? 'selected' : ''} ${auSelected ? 'au-selected' : ''}`}
    onClick={onClick}
    style={style}
  >
    {icon && <MI name={icon} size={18} />}
    {children}
  </button>
);

const IconBtn = ({ icon, onClick, title, style }) => (
  <button className="icon-btn" onClick={onClick} title={title} style={style}>
    <MI name={icon} size={22} />
  </button>
);

const Stars = ({ count, fusion, dismissed }) => {
  if (dismissed) return (
    <div className="stars"><MI name="badge" fill color={STAR_COLORS[2]} /></div>
  );
  const color = STAR_COLORS[Math.min(4, count)] || STAR_COLORS[1];
  return (
    <div className="stars">
      {Array.from({ length: Math.max(1, count) }).map((_, i) =>
        <MI key={i} name="star" fill color={color} />
      )}
    </div>
  );
};

// Character portrait — uses real asset for hero character, gradient placeholder otherwise.
function PortraitBg({ character }) {
  if (character.image) {
    return (
      <div className="portrait" style={{
        backgroundImage: `url(${character.image})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center top',
      }} />
    );
  }
  // Gradient + name fallback that hints at the character's vibe
  return (
    <div className="portrait" style={{
      background: character.bg || 'linear-gradient(160deg,#3a2f5e 0%,#7d6b9e 60%,#bdb3ff 110%)',
    }}>
      {character.placeholder || character.name}
    </div>
  );
}

const CharacterCard = ({ character, onClick, style }) => {
  const cls = ['char-card'];
  if (character.fusion) cls.push('fusion');
  if (character.dismissed) cls.push('dismissed');
  return (
    <div className={cls.join(' ')} onClick={onClick} style={style}>
      <div className="img-wrap">
        <PortraitBg character={character} />
      </div>
      {character.au && (
        <div className="au-badge" style={{ borderColor: character.auColor || '#64DCB4', color: character.auColor || '#64DCB4' }}>
          {character.au}
        </div>
      )}
      {!character.au && character.auAvailable && character.auAvailable.length > 0 && (
        <div className="au-dots">
          {character.auAvailable.map((c, i) =>
            <span key={i} style={{ background: c }} />
          )}
        </div>
      )}
      <div className="info">
        <h3>{character.name}</h3>
        <Stars count={character.star} fusion={character.fusion} dismissed={character.dismissed} />
      </div>
    </div>
  );
};

const GlassPanel = ({ children, className = '', style }) => (
  <div className={`glass ${className}`} style={style}>{children}</div>
);

// Export to window so sibling files can use them
Object.assign(window, { MI, Chip, IconBtn, Stars, CharacterCard, GlassPanel, STAR_COLORS });
