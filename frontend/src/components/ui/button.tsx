import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-sm font-semibold transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/30 disabled:pointer-events-none disabled:opacity-50 active:translate-y-px',
  {
    variants: {
      variant: {
        default: 'bg-accent text-white shadow-sm hover:brightness-105',
        secondary: 'border border-line bg-panel text-ink hover:bg-black/[.035] dark:hover:bg-white/[.06]',
        ghost: 'text-muted hover:bg-black/[.045] hover:text-ink dark:hover:bg-white/[.06]',
        outline: 'border border-line bg-transparent text-ink hover:bg-black/[.035] dark:hover:bg-white/[.06]',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-9 rounded-lg px-3 text-xs',
        icon: 'size-10 rounded-xl p-0',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  },
);

export type ButtonProps = Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'size'> & VariantProps<typeof buttonVariants>;

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button ref={ref} className={cn(buttonVariants({ variant, size, className }))} {...props} />
  ),
);
Button.displayName = 'Button';
