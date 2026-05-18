// Tournament screen — setup card + 1v1 voting + winner screen.

const { useState: useStateT } = React;

const TOURNAMENT_MODES = [
  { id: 'random',   label: 'Random',         icon: 'shuffle' },
  { id: 'all',      label: 'All',            icon: 'apps' },
  { id: 'stars_1',  label: '1★ Only',        icon: 'filter_1', color: '#fb923c' },
  { id: 'stars_2',  label: '2★ Only',        icon: 'filter_2', color: '#94a3b8' },
  { id: 'stars_3',  label: '3★ Only',        icon: 'filter_3', color: '#fbbf24' },
  { id: 'fusions',  label: 'Fusions',        icon: 'filter_4', color: '#e879f9' },
];

const TOURNAMENT_CATEGORIES = ['Favourite', 'Strongest', 'Cutest', 'Most Heroic'];

function ModeChips({ value, onChange }) {
  return (
    <div className="mode-grid">
      {TOURNAMENT_MODES.map(m => (
        <button
          key={m.id}
          type="button"
          className={`mode-chip ${value === m.id ? 'selected' : ''}`}
          onClick={() => onChange(m.id)}
        >
          <MI name={m.icon} size={18} color={value === m.id ? undefined : m.color} />
          {m.label}
        </button>
      ))}
      <button
        type="button"
        className={`mode-chip ${value === 'absolute_war' ? 'selected' : ''}`}
        style={{ gridColumn: 'span 2' }}
        onClick={() => onChange('absolute_war')}
      >
        <MI name="whatshot" size={18} color={value === 'absolute_war' ? undefined : '#f87171'} />
        ABSOLUTE WAR
      </button>
      {AUS.filter(a => !a.locked).map(au => (
        <button
          key={au.id}
          type="button"
          className={`mode-chip au-chip ${value === `au_${au.id}` ? 'selected' : ''}`}
          onClick={() => onChange(`au_${au.id}`)}
          style={value === `au_${au.id}` ? { borderColor: au.color, background: `${au.color}1e`, color: au.color } : {}}
        >
          <MI name={au.icon} size={18} color={au.color} />
          <span style={{ color: au.color }}>{au.name}</span>
        </button>
      ))}
    </div>
  );
}

function TournamentSetup({ onStart }) {
  const [mode, setMode] = useStateT('random');
  const [count, setCount] = useStateT(16);
  const [category, setCategory] = useStateT(TOURNAMENT_CATEGORIES[0]);

  return (
    <div className="setup-card">
      <h2 style={{
        fontSize: '1.5rem', fontWeight: 800,
        marginBottom: 24, marginTop: 0,
        textAlign: 'center',
      }}>Tournament Setup</h2>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <div>
          <div className="form-label">Mode</div>
          <ModeChips value={mode} onChange={setMode} />
        </div>

        {mode === 'random' && (
          <div>
            <label className="form-label" htmlFor="char-count" style={{ textTransform: 'none', letterSpacing: 0, fontSize: 13 }}>Random character count</label>
            <input
              id="char-count"
              type="number"
              className="form-input"
              value={count}
              min="4" max="128"
              onChange={e => setCount(parseInt(e.target.value, 10))}
            />
          </div>
        )}

        <div>
          <div className="form-label">Category</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            {TOURNAMENT_CATEGORIES.map(c => (
              <button
                key={c}
                type="button"
                className={`mode-chip ${category === c ? 'selected' : ''}`}
                onClick={() => setCategory(c)}
              >
                <MI name="label" size={18} />
                {c}
              </button>
            ))}
          </div>
        </div>

        <button className="btn btn-primary" style={{ width: '100%', fontSize: 15 }} onClick={() => onStart({ mode, count, category })}>
          <MI name="sports_esports" size={18} />Start Tournament
        </button>
      </div>
    </div>
  );
}

function TournamentMatch({ contestants, round, matchIdx, totalMatches, onPick, category }) {
  const [c1, c2] = contestants;
  const pct = (matchIdx / Math.max(1, totalMatches)) * 100;
  return (
    <div style={{ maxWidth: 768, margin: '0 auto', width: '100%' }}>
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        <h2 style={{
          fontSize: 24, fontWeight: 800,
          color: 'var(--primary)',
          margin: 0,
          fontFamily: "'Metamorphous', serif",
        }}>{round}</h2>
        <p style={{
          margin: '6px 0 0', fontSize: 14,
          color: 'var(--on-surface-var)',
        }}>Match {matchIdx + 1} of {totalMatches}</p>
        <p style={{
          margin: '4px 0 0', fontSize: 13, fontWeight: 600,
          color: 'var(--on-surface-var)',
        }}>Category: {category}</p>
        <div className="progress-bar" style={{ marginTop: 12, maxWidth: 240, margin: '12px auto 0' }}>
          <div className="progress-fill" style={{ width: `${pct}%` }} />
        </div>
      </div>

      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        gap: 24, flexWrap: 'wrap',
      }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
          <div className="contestant-card" onClick={() => onPick(0)}>
            {c1.image
              ? <div style={{ width: '100%', height: '100%', backgroundImage: `url(${c1.image})`, backgroundSize: 'cover', backgroundPosition: 'center top' }} />
              : <div className="portrait" style={{ background: c1.bg, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <MI name="person" size={60} style={{ color: 'rgba(255,255,255,0.32)' }} />
                </div>
            }
          </div>
          <h3 style={{ fontSize: 17, fontWeight: 700, margin: 0, textAlign: 'center' }}>
            {c1.name} <span style={{ opacity: 0.6, fontWeight: 600 }}>(3★)</span>
          </h3>
        </div>

        <div className="vs-bubble">VS</div>

        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
          <div className="contestant-card" onClick={() => onPick(1)}>
            <div className="portrait" style={{ background: c2.bg, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <MI name="person" size={60} style={{ color: 'rgba(255,255,255,0.32)' }} />
            </div>
          </div>
          <h3 style={{ fontSize: 17, fontWeight: 700, margin: 0, textAlign: 'center' }}>
            {c2.name} <span style={{ opacity: 0.6, fontWeight: 600 }}>(2★)</span>
          </h3>
        </div>
      </div>

      <p style={{
        textAlign: 'center', fontSize: 13, marginTop: 18,
        color: '#fcd34d', opacity: 0.75,
      }}>Sign in to save wins to the leaderboard.</p>
    </div>
  );
}

function TournamentScreen() {
  const [state, setState] = useStateT('setup'); // setup | match
  const [category, setCategory] = useStateT('Favourite');

  const contestants = [
    CHARACTERS.find(c => c.id === 'aria'),
    CHARACTERS.find(c => c.id === 'mira'),
  ];

  return (
    <div className="tournament-shell">
      <header style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        paddingTop: 24, paddingBottom: 18, marginBottom: 24,
        borderBottom: '1px solid var(--outline-var)', gap: 16, flexWrap: 'wrap',
      }}>
        <div>
          <h1 style={{
            fontSize: '2.25rem', fontWeight: 800, margin: 0,
            color: 'var(--primary)', fontFamily: "'Metamorphous', serif",
          }}>Character Tournament</h1>
          <p style={{
            margin: '4px 0 0', fontSize: 15,
            color: 'var(--on-surface-var)',
          }}>Pick your favourite</p>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button className="btn btn-secondary" onClick={() => setState('setup')}>
            <MI name="arrow_back" size={18} />Back
          </button>
          <button className="btn btn-secondary">
            <MI name="leaderboard" size={18} />Leaderboard
          </button>
          <button className="btn btn-secondary">
            <MI name="login" size={18} />Sign In
          </button>
        </div>
      </header>

      {state === 'setup' && (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '24px 0' }}>
          <TournamentSetup onStart={(s) => { setCategory(s.category); setState('match'); }} />
        </div>
      )}

      {state === 'match' && (
        <div style={{ padding: '24px 0' }}>
          <TournamentMatch
            contestants={contestants}
            round="Round 1"
            matchIdx={2}
            totalMatches={8}
            category={category}
            onPick={() => {}}
          />
        </div>
      )}
    </div>
  );
}

Object.assign(window, { TournamentScreen });
