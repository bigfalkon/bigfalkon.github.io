// Hub / Links screen — the launcher

function HubScreen({ onNavigate }) {
  const items = [
    { id: 'gallery',     icon: 'collections',           title: 'Gallery',          desc: 'Browse all characters' },
    { id: 'admin',       icon: 'admin_panel_settings', title: 'Admin Panel',      desc: 'Manage characters & universes' },
    { id: 'tournament',  icon: 'sports_esports',        title: 'Tournament',       desc: 'Pick your favourite' },
    { id: 'random',      icon: 'casino',                title: 'Random Picker',    desc: 'Draw random character pairs' },
  ];

  return (
    <div style={{
      maxWidth: 760, margin: '0 auto',
      padding: '60px 24px',
      display: 'flex', flexDirection: 'column', gap: 14,
    }}>
      <div style={{ textAlign: 'center', marginBottom: 16 }}>
        <h1 style={{
          fontSize: '2.25rem', fontWeight: 800, margin: 0,
          color: 'var(--primary)', fontFamily: "'Metamorphous', serif",
        }}>Karakter Evreni</h1>
        <p style={{
          margin: '6px 0 0', fontSize: 14,
          color: 'var(--on-surface-var)',
        }}>Choose a destination</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
        {items.map(it => (
          <div
            key={it.id}
            className="hub-card"
            onClick={() => onNavigate && onNavigate(it.id)}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <MI name={it.icon} size={26} style={{ color: 'rgba(189,179,255,0.7)' }} />
              <div>
                <div style={{ fontWeight: 800, fontSize: 17, lineHeight: 1.15 }}>{it.title}</div>
                <div style={{
                  fontSize: 12, marginTop: 2,
                  color: 'var(--on-surface-var)',
                }}>{it.desc}</div>
              </div>
            </div>
            <span className="hub-arrow material-symbols-outlined" style={{ fontSize: 22 }}>chevron_right</span>
          </div>
        ))}
      </div>

      <button style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        marginTop: 20, padding: '10px 16px',
        background: 'transparent',
        border: '1px dashed var(--outline-var)',
        borderRadius: 12,
        color: 'var(--on-surface-var)',
        cursor: 'pointer',
        fontFamily: 'inherit', fontSize: 14, fontWeight: 600,
      }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <MI name="developer_mode" size={18} />
          Developer Tools
        </span>
        <MI name="expand_more" size={18} />
      </button>

      <div style={{
        marginTop: 40, textAlign: 'center',
        fontSize: 11, color: 'var(--outline-var)',
      }}>© Karakter Evreni</div>
    </div>
  );
}

Object.assign(window, { HubScreen });
