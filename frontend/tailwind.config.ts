import type { Config } from "tailwindcss";
import tailwindcssAnimate from "tailwindcss-animate";

export default {
  darkMode: ["class"],
  content: ["./pages/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}", "./app/**/*.{ts,tsx}", "./src/**/*.{ts,tsx}"],
  prefix: "",
  theme: {
  	container: {
  		center: true,
  		padding: '2rem',
  		screens: {
  			'2xl': '1400px'
  		}
  	},
	extend: {
		screens: {
			// 1920px monitors at Windows 150% scaling leave a ~1265px viewport
			// (after scrollbar/window chrome); keep wide layouts active there.
			xl: '1200px',
		},
		fontSize: {
			'page-title': ['var(--page-title-size)', { lineHeight: '1.1875' }],
			'form-title': ['var(--form-title-size)', { lineHeight: '1.2' }],
			'section-title': ['var(--section-title-size)', { lineHeight: '1.25' }],
			'subsection-title': ['var(--subsection-title-size)', { lineHeight: '1.3' }],
			'marketing-title': ['var(--marketing-title-size)', { lineHeight: '1.2' }],
			'home-section-title': ['var(--home-section-title-size)', { lineHeight: '1.25' }],
			'hero-title': ['var(--hero-title-size)', { lineHeight: '1.25' }],
			'metric': ['var(--metric-size)', { lineHeight: '1' }],
		},
		spacing: {
			card: 'var(--card-padding)',
			section: 'var(--section-gap)',
			'marketing-section': 'var(--marketing-section-padding)',
			header: 'var(--app-header-height)',
			gutter: 'var(--page-gutter)',
			'page-y': 'var(--page-padding-y)',
			grid: 'var(--grid-gap)',
		},
		maxWidth: {
		  app: '77.5rem',      // 1240
			admin: '100rem',     // 1600
			marketing: '97.5rem', // 1560
			page: '100rem'
		},
  		colors: {
  			border: 'hsl(var(--border))',
  			input: 'hsl(var(--input))',
  			ring: 'hsl(var(--ring))',
      placeholder: 'hsl(var(--placeholder))',
      disabled: {
        DEFAULT: 'hsl(var(--disabled))',
        foreground: 'hsl(var(--disabled-foreground))'
      },
  			background: 'hsl(var(--background))',
  			foreground: 'hsl(var(--foreground))',
  			primary: {
					DEFAULT: 'hsl(var(--primary))',
					foreground: 'hsl(var(--primary-foreground))',
					strong: 'hsl(var(--primary-strong))',
				},
  			secondary: {
  				DEFAULT: 'hsl(var(--secondary))',
  				foreground: 'hsl(var(--secondary-foreground))'
  			},
  			muted: {
  				DEFAULT: 'hsl(var(--muted))',
  				foreground: 'hsl(var(--muted-foreground))'
  			},
  			accent: {
  				DEFAULT: 'hsl(var(--accent))',
  				foreground: 'hsl(var(--accent-foreground))'
  			},
  			success: {
					DEFAULT: 'hsl(var(--success))',
					foreground: 'hsl(var(--success-foreground))',
					subtle: 'hsl(var(--success-subtle))',
					strong: 'hsl(var(--success-strong))',
					border: 'hsl(var(--success-border))',
				},

				warning: {
					DEFAULT: 'hsl(var(--warning))',
					foreground: 'hsl(var(--warning-foreground))',
					subtle: 'hsl(var(--warning-subtle))',
					strong: 'hsl(var(--warning-strong))',
					border: 'hsl(var(--warning-border))',
				},

				destructive: {
					DEFAULT: 'hsl(var(--destructive))',
					foreground: 'hsl(var(--destructive-foreground))',
					subtle: 'hsl(var(--destructive-subtle))',
					strong: 'hsl(var(--destructive-strong))',
					border: 'hsl(var(--danger-border))',
				},

				info: {
					DEFAULT: 'hsl(var(--info))',
					foreground: 'hsl(var(--info-foreground))',
					subtle: 'hsl(var(--info-subtle))',
					strong: 'hsl(var(--info-strong))',
					border: 'hsl(var(--info-border))',
				},

				purple: {
					DEFAULT: 'hsl(var(--purple))',
					subtle: 'hsl(var(--purple-subtle))',
				},
  			popover: {
  				DEFAULT: 'hsl(var(--popover))',
  				foreground: 'hsl(var(--popover-foreground))'
  			},
  			card: {
  				DEFAULT: 'hsl(var(--card))',
  				foreground: 'hsl(var(--card-foreground))'
  			},
  			sidebar: {
  				DEFAULT: 'hsl(var(--sidebar-background))',
  				foreground: 'hsl(var(--sidebar-foreground))',
  				primary: 'hsl(var(--sidebar-primary))',
  				'primary-foreground': 'hsl(var(--sidebar-primary-foreground))',
  				accent: 'hsl(var(--sidebar-accent))',
  				'accent-foreground': 'hsl(var(--sidebar-accent-foreground))',
  				border: 'hsl(var(--sidebar-border))',
  				ring: 'hsl(var(--sidebar-ring))'
  			},
  			dashboard: {
  				bg: 'hsl(var(--dashboard-bg))'
  			},
  			chart: {
  				'1': 'hsl(var(--chart-1))',
  				'2': 'hsl(var(--chart-2))',
  				'3': 'hsl(var(--chart-3))',
  				'4': 'hsl(var(--chart-4))',
  				'5': 'hsl(var(--chart-5))'
  			}
  		},
  		borderRadius: {
  			lg: 'var(--radius)',
  			md: 'calc(var(--radius) - 2px)',
  			sm: 'calc(var(--radius) - 4px)'
  		},
      boxShadow: {
        'app-sm': 'var(--shadow-sm)',
        'app-md': 'var(--shadow-md)',
        marketing: 'var(--shadow-marketing)'
      },
  		keyframes: {
  			'accordion-down': {
  				from: {
  					height: '0'
  				},
  				to: {
  					height: 'var(--radix-accordion-content-height)'
  				}
  			},
  			'accordion-up': {
  				from: {
  					height: 'var(--radix-accordion-content-height)'
  				},
  				to: {
  					height: '0'
  				}
  			}
  		},
  		animation: {
  			'accordion-down': 'accordion-down 0.2s ease-out',
  			'accordion-up': 'accordion-up 0.2s ease-out'
  		}
  	}
  },
	plugins: [tailwindcssAnimate],
} satisfies Config;
