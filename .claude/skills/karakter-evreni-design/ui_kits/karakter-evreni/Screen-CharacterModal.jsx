// Character detail modal — shows evolutions (1★ / 2★ / 3★) + fusions.

function EvolutionCard({ star, image, bg, label, onClick, highlighted }) {
  const cls = ['char-card'];
  if (highlighted) cls.push('fusion');
  return (
    <div
      className={cls.join(' ')}
      style={{
        width: '100%', maxWidth: 220,
        ...(highlighted ? { borderColor: 'var(--primary)', boxShadow: '0 0 0 1px var(--primary)' } : {}),
      }}
      onClick={onClick}
    >
      <div className="img-wrap">
        {image
          ? <div className="portrait" style={{ backgroundImage: `url(${image})`, backgroundSize: 'cover', backgroundPosition: 'center top' }} />
          : <div className="portrait" style={{ background: bg, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <MI name="person" size={48} style={{ color: 'rgba(255,255,255,0.32)' }} />
            </div>
        }
      </div>
      <div className="info">
        <h3>{label}</h3>
        <Stars count={star} />
      </div>
    </div>
  );
}

function FusionCard({ partner, onClick }) {
  return (
    <div
      className="char-card fusion"
      onClick={onClick}
      style={{ width: '100%' }}
    >
      <div className="img-wrap">
        <div className="portrait" style={{
          background: partner.bg,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <MI name="merge" size={36} style={{ color: 'rgba(255,255,255,0.32)' }} />
        </div>
      </div>
      <div className="info">
        <h3>{partner.name}</h3>
        <Stars count={4} fusion />
      </div>
    </div>
  );
}

function CharacterModal({ character, onClose, onNext, onPrev }) {
  if (!character) return null;

  // Mock evolutions for the character
  const evolutions = [
    { star: 1, image: character.image, bg: character.bg, label: character.name },
    { star: 2, image: character.image, bg: character.bg, label: character.name },
    { star: 3, image: character.image, bg: character.bg, label: character.name },
  ];

  const fusions = [
    { name: `${character.name.split(' ')[0]} × Mira`, bg: 'linear-gradient(160deg,#3a285a,#8a5e9e 60%,#ffb8a8 130%)' },
    { name: `${character.name.split(' ')[0]} × Kael`, bg: 'linear-gradient(160deg,#3a1e5e,#7a3aa5 50%,#e0b3ff 130%)' },
    { name: `${character.name.split(' ')[0]} × Soren`, bg: 'linear-gradient(160deg,#3a1e1e,#7a3030 50%,#ff8a5b 130%)' },
  ];

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-dialog glass-dialog" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h2 style={{ color: 'var(--on-surface)' }}>{character.name}</h2>
            <p style={{
              fontSize: 13, margin: '6px 0 0',
              color: 'var(--on-surface-var)',
            }}>İnsan · Ateş Krallığı</p>
          </div>
          <div style={{ display: 'flex', gap: 4 }}>
            <button className="icon-btn" title="View in AU" style={{
              border: '1px solid rgba(100,220,180,0.5)',
              background: 'rgba(100,220,180,0.08)',
              color: 'var(--au)',
              width: 40, height: 40,
            }}>
              <MI name="auto_fix_high" size={20} />
            </button>
            <IconBtn icon="close" onClick={onClose} />
          </div>
        </div>

        <div className="modal-content">
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, minmax(0, 220px))',
            gap: 16, justifyContent: 'center', marginBottom: 32,
          }}>
            {evolutions.map(evo => (
              <EvolutionCard
                key={evo.star}
                star={evo.star}
                image={evo.image}
                bg={evo.bg}
                label={evo.label}
                highlighted={evo.star === 3}
              />
            ))}
          </div>

          <h3 style={{
            fontFamily: "'Metamorphous', serif",
            fontSize: 20,
            color: 'var(--fusion)',
            textAlign: 'center',
            marginBottom: 16,
          }}>Fusions</h3>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
            gap: 14,
            maxWidth: 580,
            margin: '0 auto',
          }}>
            {fusions.map((f, i) => (
              <FusionCard key={i} partner={f} />
            ))}
          </div>
        </div>

        <div className="modal-footer">
          <button className="modal-nav-btn" onClick={onPrev}>
            <MI name="arrow_back" size={18} />
            <span>Kael Vance</span>
          </button>
          <button className="modal-nav-btn" onClick={onNext}>
            <span>Mira Solace</span>
            <MI name="arrow_forward" size={18} />
          </button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { CharacterModal });
