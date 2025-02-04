import * as AlertDialogPrimitive from '@radix-ui/react-alert-dialog';
import { cn } from '@/shared/lib/utils';
import { buttonVariants } from '@/shared/ui/shadcn/button';

const AlertDialog = AlertDialogPrimitive.Root;
const AlertDialogTrigger = ({
    className,
    ...props
}: React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Trigger>) => (
    <AlertDialogPrimitive.Trigger
        asChild
        className={cn('w-fit', className)}
        {...props}
    />
);
const AlertDialogPortal = AlertDialogPrimitive.Portal;

const AlertDialogOverlay = ({
    className,
    ...props
}: React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Overlay>) => (
    <AlertDialogPrimitive.Overlay
        className={cn(
            'fixed inset-0 z-50 bg-black/80 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
            className
        )}
        {...props}
    />
);

const AlertDialogContent = ({
    className,
    ...props
}: React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Content>) => (
    <AlertDialogPrimitive.Content
        className={cn(
            'fixed left-[50%] top-[50%] z-50 grid w-full max-w-md translate-x-[-50%] translate-y-[-50%] gap-4 border bg-background p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] rounded-lg flex-col gap-12 wrap-pretty',
            className
        )}
        {...props}
    />
);

const AlertDialogHeader = ({
    className,
    ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
    <div
        className={cn(
            'flex flex-col space-y-2 text-center sm:text-left',
            className
        )}
        {...props}
    />
);

const AlertDialogFooter = ({
    className,
    ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
    <div
        className={cn('flex align-middle justify-evenly', className)}
        {...props}
    />
);

const AlertDialogTitle = ({
    className,
    ...props
}: React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Title>) => (
    <AlertDialogPrimitive.Title
        className={cn('text-lg text-center font-semibold', className)}
        {...props}
    />
);

const AlertDialogDescription = ({
    className,
    ...props
}: React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Description>) => (
    <AlertDialogPrimitive.Description
        className={cn(
            'text-sm text-muted-foreground text-center text-pretty',
            className
        )}
        {...props}
    />
);

const AlertDialogAction = ({
    className,
    ...props
}: React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Action>) => (
    <AlertDialogPrimitive.Action
        className={cn(buttonVariants({ variant: 'pass' }), className)}
        {...props}
    />
);

const AlertDialogCancel = ({
    className,
    ...props
}: React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Cancel>) => (
    <AlertDialogPrimitive.Cancel
        className={cn(buttonVariants({ variant: 'warn' }), className)}
        {...props}
    />
);

export {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogOverlay,
    AlertDialogPortal,
    AlertDialogTitle,
    AlertDialogTrigger,
};
