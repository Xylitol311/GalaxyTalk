import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/shared/lib/utils';

const buttonVariants = cva(
    'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-bold transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0',
    {
        variants: {
            variant: {
                default:
                    'bg-primary text-primary-foreground shadow hover:bg-primary/90',
                warn: 'border border-input bg-background text-black shadow-sm hover:bg-secondary/90 [&_svg]:text-destructive',
                pass: 'border border-input bg-background text-black shadow-sm hover:bg-secondary/90 [&_svg]:text-blue-600',
                confirm:
                    'border border-input bg-background text-black shadow-sm hover:bg-secondary/90 [&_svg]:text-green-600',
                outline:
                    'border border-input bg-background shadow-sm hover:bg-accent hover:text-accent-foreground',
                secondary:
                    'border border-input bg-secondary text-secondary-foreground shadow-sm hover:bg-secondary/75',
                ghost: 'hover:bg-accent hover:text-accent-foreground',
                link: 'text-primary underline-offset-4 hover:underline',
            },
            size: {
                default: 'h-9 px-4 py-2 rounded-full',
                icon: 'w-9 h-9 p-2 rounded-full',
                m_md: 'h-9 px-4 py-2 rounded-md',
                m_lg: 'w-28 min-h-20 p-4 flex flex-col items-center justify-center gap-2 text-center text-black [&_svg]:size-12 [&_svg]:shrink-0',
            },
        },
        defaultVariants: {
            variant: 'default',
            size: 'default',
        },
    }
);

export interface ButtonProps
    extends React.ButtonHTMLAttributes<HTMLButtonElement>,
        VariantProps<typeof buttonVariants> {
    asChild?: boolean;
}

const Button = ({
    className,
    variant,
    size,
    asChild = false,
    ...props
}: ButtonProps & { ref?: React.Ref<HTMLButtonElement> }) => {
    const Comp = asChild ? Slot : 'button';
    return (
        <Comp
            className={cn(buttonVariants({ variant, size, className }))}
            {...props}
        />
    );
};

export { Button, buttonVariants };
