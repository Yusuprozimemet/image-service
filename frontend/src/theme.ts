import { createTheme, alpha } from '@mui/material/styles'

// A light, game-ish theme: bright greens, fat rounded corners, and buttons with
// a solid bottom edge that compresses when pressed. Everything visual lives here
// so the components stay about behaviour.

const GREEN = '#58CC02'
const GREEN_EDGE = '#46A302'
const BLUE = '#1CB0F6'
const BLUE_EDGE = '#1899D6'
const RED = '#FF4B4B'
const RED_EDGE = '#EA2B2B'
const YELLOW = '#FFC800'

const INK = '#3C3C3C'
const INK_SOFT = '#777777'
const BORDER = '#E5E5E5'
const CANVAS = '#FFFFFF'
const CANVAS_SOFT = '#F7F7F7'

// The 3D press: a solid shadow acting as the button's bottom edge, which shrinks
// as the button slides down. Nothing moves the surrounding layout — the padding
// absorbs it — so a row of buttons stays put while one is pressed.
function chunky(edge: string) {
  return {
    boxShadow: `0 4px 0 ${edge}`,
    '&:hover': { boxShadow: `0 4px 0 ${edge}` },
    '&:active': { transform: 'translateY(4px)', boxShadow: `0 0 0 ${edge}` },
    '&.Mui-disabled': { boxShadow: `0 4px 0 ${alpha(edge, 0.35)}` },
  }
}

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: GREEN, dark: GREEN_EDGE, contrastText: '#FFFFFF' },
    secondary: { main: BLUE, dark: BLUE_EDGE, contrastText: '#FFFFFF' },
    error: { main: RED, dark: RED_EDGE },
    warning: { main: YELLOW },
    success: { main: GREEN },
    background: { default: CANVAS, paper: CANVAS },
    text: { primary: INK, secondary: INK_SOFT },
    divider: BORDER,
  },

  shape: { borderRadius: 16 },

  typography: {
    fontFamily: 'Nunito, system-ui, "Segoe UI", Roboto, sans-serif',
    h1: { fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.02em' },
    h2: { fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.01em' },
    h3: { fontSize: '1.125rem', fontWeight: 800 },
    body1: { fontWeight: 600 },
    body2: { fontWeight: 600 },
    button: { fontWeight: 800, fontSize: '0.95rem', letterSpacing: '0.02em' },
  },

  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: { backgroundColor: CANVAS_SOFT },
      },
    },

    MuiButton: {
      defaultProps: { disableElevation: true, variant: 'contained' },
      styleOverrides: {
        root: {
          borderRadius: 14,
          padding: '10px 22px',
          textTransform: 'uppercase',
          transition: 'transform 80ms ease, box-shadow 80ms ease, background-color 120ms ease',
          // Only the filled buttons get the 3D edge; text buttons stay flat so
          // the nav does not look like a row of blocks.
          variants: [
            { props: { variant: 'contained', color: 'primary' }, style: chunky(GREEN_EDGE) },
            { props: { variant: 'contained', color: 'secondary' }, style: chunky(BLUE_EDGE) },
            { props: { variant: 'contained', color: 'error' }, style: chunky(RED_EDGE) },
          ],
        },
        outlined: {
          borderWidth: 2,
          borderBottomWidth: 4,
          '&:hover': { borderWidth: 2, borderBottomWidth: 4, backgroundColor: CANVAS_SOFT },
          '&:active': { transform: 'translateY(2px)', borderBottomWidth: 2 },
        },
        text: { textTransform: 'none', fontWeight: 800 },
      },
    },

    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { border: `2px solid ${BORDER}` },
      },
    },

    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          border: `2px solid ${BORDER}`,
          overflow: 'hidden',
          transition: 'transform 120ms ease, border-color 120ms ease',
        },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 800, borderRadius: 999 },
        outlined: { borderWidth: 2 },
      },
    },

    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 14,
          backgroundColor: CANVAS,
          '& fieldset': { borderWidth: 2, borderColor: BORDER },
          '&:hover fieldset': { borderColor: '#D0D0D0' },
          '&.Mui-focused fieldset': { borderWidth: 2, borderColor: BLUE },
        },
        input: { fontWeight: 600 },
      },
    },

    MuiInputLabel: { styleOverrides: { root: { fontWeight: 700 } } },

    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 14, fontWeight: 700, border: '2px solid transparent' },
      },
    },

    MuiAppBar: {
      defaultProps: { elevation: 0, color: 'inherit' },
      styleOverrides: {
        root: { backgroundColor: CANVAS, borderBottom: `2px solid ${BORDER}` },
      },
    },

    MuiTooltip: {
      styleOverrides: {
        tooltip: { fontWeight: 700, borderRadius: 10, fontSize: '0.8rem' },
      },
    },
  },
})
